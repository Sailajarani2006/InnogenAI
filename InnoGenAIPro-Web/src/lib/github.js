import { db } from './firebase';
import { doc, updateDoc } from 'firebase/firestore';

const GITHUB_CLIENT_ID = 'Ov23lioj0t4mesvYpTDv';
const GITHUB_CLIENT_SECRET = '511c3c13e5fd600e3c136ef6654378ba207c19db';

/**
 * Request Device Code from GitHub (Official Device Flow)
 */
export const requestDeviceCode = async () => {
  const res = await fetch('/api/github/device', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    },
    body: JSON.stringify({
      client_id: GITHUB_CLIENT_ID,
      scope: 'repo,user'
    })
  });

  if (!res.ok) {
    const err = await res.json();
    throw new Error(err.message || "Failed to initiate GitHub authorization.");
  }

  return await res.json();
};

/**
 * Poll GitHub until user authorizes the code on github.com/login/device
 */
export const pollDeviceToken = async (deviceCode, interval = 5, onTimeout) => {
  const pollInterval = Math.max(interval, 4) * 1000;
  const maxAttempts = 60; // 5 mins
  let attempts = 0;

  while (attempts < maxAttempts) {
    await new Promise((r) => setTimeout(r, pollInterval));
    attempts++;

    try {
      const res = await fetch('/api/github/token', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify({
          client_id: GITHUB_CLIENT_ID,
          device_code: deviceCode,
          grant_type: 'urn:ietf:params:oauth:grant-type:device_code'
        })
      });

      const data = await res.json();

      if (data.access_token) {
        const accessToken = data.access_token;
        // Fetch user profile
        const userRes = await fetch('https://api.github.com/user', {
          headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Accept': 'application/vnd.github.v3+json'
          }
        });

        const userData = userRes.ok ? await userRes.json() : null;
        const username = userData?.login || 'GitHub User';

        sessionStorage.setItem('github_access_token', accessToken);
        sessionStorage.setItem('github_username', username);

        return { token: accessToken, username, user: userData };
      }

      if (data.error === 'authorization_pending') {
        // User hasn't clicked authorize yet, continue polling
        continue;
      } else if (data.error === 'slow_down') {
        // Slow down polling
        await new Promise((r) => setTimeout(r, 5000));
        continue;
      } else if (data.error === 'expired_token') {
        throw new Error("Authorization code expired. Please click connect again.");
      } else if (data.error === 'access_denied') {
        throw new Error("Access was denied on GitHub.");
      } else if (data.error) {
        throw new Error(data.error_description || data.error);
      }
    } catch (err) {
      if (err.message && !err.message.includes('authorization_pending')) {
        throw err;
      }
    }
  }

  throw new Error("Authorization timed out. Please try again.");
};

/**
 * Get cached token from session
 */
export const getStoredGitHubToken = () => {
  return sessionStorage.getItem('github_access_token');
};

export const getStoredGitHubUsername = () => {
  return sessionStorage.getItem('github_username');
};

/**
 * Helper to safely encode UTF-8 strings to Base64 in browser
 */
const utf8ToBase64 = (str) => {
  return window.btoa(encodeURIComponent(str).replace(/%([0-9A-F]{2})/g, (match, p1) => {
    return String.fromCharCode(parseInt(p1, 16));
  }));
};

/**
 * Push all project code files to a GitHub repository
 */
export const pushProjectToGitHub = async ({
  project,
  repoName,
  isPrivate = false,
  token,
  onStatusUpdate
}) => {
  if (!token) {
    throw new Error("GitHub token is required. Please log in with GitHub.");
  }

  const updateStatus = (msg) => {
    if (onStatusUpdate) onStatusUpdate(msg);
  };

  // 1. Verify token & get authenticated user
  updateStatus("Connecting to your GitHub account...");
  const userRes = await fetch('https://api.github.com/user', {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/vnd.github.v3+json'
    }
  });

  if (!userRes.ok) {
    sessionStorage.removeItem('github_access_token');
    const err = await userRes.json();
    throw new Error(`GitHub Authentication failed: ${err.message || 'Invalid or expired token. Please log in again.'}`);
  }

  const userData = await userRes.json();
  const owner = userData.login;

  // Sanitize repo name
  const rawName = repoName || project.name || project.title || project.idea || 'innogen-ai-app';
  const cleanRepoName = rawName
    .toLowerCase()
    .replace(/[^a-z0-9_-]/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '') || 'innogen-ai-app';

  // 2. Create or find repo
  updateStatus(`Creating repository "${cleanRepoName}" on GitHub...`);
  let repoData;
  const createRes = await fetch('https://api.github.com/user/repos', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/vnd.github.v3+json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      name: cleanRepoName,
      description: project.description || "Generated via InnoGen AI Pro",
      private: !!isPrivate,
      auto_init: true
    })
  });

  if (createRes.status === 422) {
    // Repository already exists
    updateStatus(`Repository "${cleanRepoName}" found, preparing commit...`);
    const checkRes = await fetch(`https://api.github.com/repos/${owner}/${cleanRepoName}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/vnd.github.v3+json'
      }
    });
    if (checkRes.ok) {
      repoData = await checkRes.json();
    } else {
      const err = await createRes.json();
      throw new Error(`GitHub error: ${err.message || 'Repository creation conflict'}`);
    }
  } else if (!createRes.ok) {
    const err = await createRes.json();
    throw new Error(`Failed to create repository: ${err.message || 'Check GitHub permissions'}`);
  } else {
    repoData = await createRes.json();
  }

  // 3. Prepare file tree
  let codeData = {};
  if (typeof project.generatedCode === 'string') {
    try { codeData = JSON.parse(project.generatedCode); } catch (e) { codeData = {}; }
  } else if (project.generatedCode) {
    codeData = project.generatedCode;
  }

  const files = [];
  const readmeContent = codeData.readme || `# ${project.title || project.name || 'InnoGen AI Project'}\n\n${project.description || 'Generated with InnoGen AI Pro'}\n\n## Tech Stack\n- Frontend: HTML/CSS/JS\n- Backend: Python FastAPI\n- Database: SQLite`;
  files.push({ path: 'README.md', content: readmeContent });

  if (codeData.frontendCode && codeData.frontendCode.trim()) {
    files.push({ path: 'frontend/index.html', content: codeData.frontendCode });
  }

  if (codeData.backendCode && codeData.backendCode.trim()) {
    files.push({ path: 'backend/main.py', content: codeData.backendCode });
  }

  if (codeData.databaseSchema && codeData.databaseSchema.trim()) {
    files.push({ path: 'database/schema.sql', content: codeData.databaseSchema });
  }

  if (codeData.dockerConfig && codeData.dockerConfig.trim()) {
    files.push({ path: 'Dockerfile', content: codeData.dockerConfig });
  }

  if (codeData.apiDocs && codeData.apiDocs.trim()) {
    files.push({ path: 'docs/API.md', content: codeData.apiDocs });
  }

  if (codeData.testCases && codeData.testCases.trim()) {
    files.push({ path: 'tests/test_main.py', content: codeData.testCases });
  }

  // 4. Push files sequentially
  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    updateStatus(`Pushing ${file.path} (${i + 1}/${files.length})...`);

    // Check if file exists to get SHA for updates
    let sha = null;
    try {
      const getFileRes = await fetch(`https://api.github.com/repos/${owner}/${cleanRepoName}/contents/${file.path}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Accept': 'application/vnd.github.v3+json'
        }
      });
      if (getFileRes.ok) {
        const fileJson = await getFileRes.json();
        sha = fileJson.sha;
      }
    } catch (e) {
      // New file, sha stays null
    }

    const payload = {
      message: `Add ${file.path} via InnoGen AI Pro`,
      content: utf8ToBase64(file.content)
    };
    if (sha) payload.sha = sha;

    const putRes = await fetch(`https://api.github.com/repos/${owner}/${cleanRepoName}/contents/${file.path}`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/vnd.github.v3+json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });

    if (!putRes.ok) {
      const err = await putRes.json();
      console.warn(`Warning uploading ${file.path}:`, err);
    }
  }

  // 5. Update project in Firestore with githubRepo URL
  if (project.id && repoData.html_url) {
    try {
      await updateDoc(doc(db, 'projects', project.id), {
        githubRepo: repoData.html_url
      });
    } catch (dbErr) {
      console.warn("Could not save githubRepo in Firestore:", dbErr);
    }
  }

  updateStatus("Complete!");
  return repoData;
};
