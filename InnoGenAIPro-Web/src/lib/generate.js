import { db, auth } from './firebase';
import { collection, doc, setDoc } from 'firebase/firestore';

const GROQ_API_KEY = import.meta.env?.VITE_GROQ_API_KEY || "";

const MODELS = [
  "openai/gpt-oss-120b",
  "openai/gpt-oss-20b",
  "qwen/qwen3.8-27b"
];

function extractAndParseJson(text) {
  let cleaned = text.trim();
  
  // Strip markdown code fences if present
  if (cleaned.startsWith('```json')) {
    cleaned = cleaned.replace(/^```json\s*/i, '').replace(/\s*```$/, '');
  } else if (cleaned.startsWith('```')) {
    cleaned = cleaned.replace(/^```\s*/, '').replace(/\s*```$/, '');
  }

  // Find boundaries of outer JSON object
  const firstBrace = cleaned.indexOf('{');
  const lastBrace = cleaned.lastIndexOf('}');
  if (firstBrace !== -1 && lastBrace !== -1 && lastBrace > firstBrace) {
    cleaned = cleaned.substring(firstBrace, lastBrace + 1);
  }

  try {
    return JSON.parse(cleaned);
  } catch (initialErr) {
    // Attempt fallback cleaning for unescaped control characters inside code strings
    try {
      const sanitized = cleaned
        .replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '');
      return JSON.parse(sanitized);
    } catch (secondErr) {
      console.warn("JSON repair attempt failed:", initialErr.message);
      throw new Error("Could not parse AI response into structured JSON. Please try again.");
    }
  }
}

export const generateApp = async (idea, userOrId) => {
  const currentAuthUser = auth.currentUser;
  const userId = (typeof userOrId === 'object' ? userOrId?.uid : userOrId) || currentAuthUser?.uid || "";
  const userEmail = (typeof userOrId === 'object' ? userOrId?.email : '') || currentAuthUser?.email || "";

  let lastError = null;

  for (const model of MODELS) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 60000); // 60s per model attempt

    try {
      const response = await fetch("https://api.groq.com/openai/v1/chat/completions", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${GROQ_API_KEY}`
        },
        signal: controller.signal,
        body: JSON.stringify({
          model: model,
          max_completion_tokens: 8192,
          messages: [
            {
              role: "system",
              content: "You are an expert full-stack developer. When given an app idea, respond with ONLY a raw JSON object containing these keys: 'name' (string), 'description' (string), 'tags' (array of strings), 'readme' (markdown string), 'frontendCode' (standalone working single-page HTML with embedded CSS and JS), 'backendCode' (complete Python FastAPI code), 'databaseSchema' (complete SQL schema). Output strictly JSON with no surrounding explanations."
            },
            {
              role: "user",
              content: `Generate a complete full-stack app for: ${idea}`
            }
          ]
        })
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        const errText = await response.text();
        console.warn(`Groq model ${model} failed (${response.status}):`, errText);
        lastError = new Error(`Groq API Status ${response.status}: ${errText}`);
        continue; // Try next fallback model
      }

      const data = await response.json();
      const content = data.choices?.[0]?.message?.content;
      if (!content) {
        throw new Error("Received empty message from AI provider.");
      }

      const generatedContent = extractAndParseJson(content);

      // Save project to Firestore
      const projectRef = doc(collection(db, 'projects'));
      const projectId = projectRef.id;
      const now = Date.now();

      const projectData = {
        id: projectId,
        userId: userId || "",
        userEmail: userEmail || "",
        title: generatedContent.name || idea,
        name: generatedContent.name || idea,
        description: generatedContent.description || `Generated application for ${idea}`,
        prompt: idea,
        idea: idea,
        status: "COMPLETE",
        createdAt: now,
        updatedAt: now,
        features: Array.isArray(generatedContent.tags) ? generatedContent.tags : ['HTML/CSS/JS', 'Python FastAPI', 'SQL'],
        tags: Array.isArray(generatedContent.tags) ? generatedContent.tags : ['HTML/CSS/JS', 'Python FastAPI', 'SQL'],
        githubRepo: "",
        hasCode: true,
        generatedCode: {
          readme: generatedContent.readme || "",
          frontendCode: generatedContent.frontendCode || "",
          backendCode: generatedContent.backendCode || "",
          databaseSchema: generatedContent.databaseSchema || ""
        },
        techStack: {
          frontend: (generatedContent.tags && generatedContent.tags[0]) || 'HTML/CSS/JS',
          backend: (generatedContent.tags && generatedContent.tags[1]) || 'Python FastAPI',
          database: 'SQLite',
          deployment: 'Docker'
        }
      };

      try {
        await setDoc(projectRef, projectData);
      } catch (saveErr) {
        console.error("Firestore project save error:", saveErr);
      }

      return { id: projectId, ...generatedContent };

    } catch (err) {
      clearTimeout(timeoutId);
      console.warn(`Attempt with ${model} failed:`, err);
      lastError = err;
    }
  }

  throw lastError || new Error("Failed to generate code from AI. Please try again.");
};
