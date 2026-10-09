import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Lock, Globe, CheckCircle2, AlertCircle, ExternalLink, X, Loader2, ArrowRight, Copy, Check } from 'lucide-react';
import GithubIcon from './GithubIcon';
import { requestDeviceCode, pollDeviceToken, getStoredGitHubToken, getStoredGitHubUsername, pushProjectToGitHub } from '../lib/github';
import './GitHubModal.css';

const GitHubModal = ({ isOpen, onClose, project, onProjectUpdated }) => {
  const [token, setToken] = useState(getStoredGitHubToken());
  const [username, setUsername] = useState(getStoredGitHubUsername());
  const [repoName, setRepoName] = useState('');
  const [isPrivate, setIsPrivate] = useState(false);
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [isPushing, setIsPushing] = useState(false);
  const [statusMessage, setStatusMessage] = useState('');
  const [error, setError] = useState('');
  const [createdRepo, setCreatedRepo] = useState(null);
  const [deviceFlow, setDeviceFlow] = useState(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (project) {
      const defaultName = (project.name || project.title || project.idea || 'innogen-app')
        .toLowerCase()
        .replace(/[^a-z0-9_-]/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-|-$/g, '');
      setRepoName(defaultName);
      setCreatedRepo(project.githubRepo ? { html_url: project.githubRepo } : null);
    }
  }, [project]);

  useEffect(() => {
    setToken(getStoredGitHubToken());
    setUsername(getStoredGitHubUsername());
    setDeviceFlow(null);
    setError('');
  }, [isOpen]);

  if (!isOpen) return null;

  const startGitHubAuth = async () => {
    setIsLoggingIn(true);
    setError('');
    setDeviceFlow(null);

    try {
      const flow = await requestDeviceCode();
      setDeviceFlow(flow);
      setIsLoggingIn(false);

      // Auto copy code
      try {
        await navigator.clipboard.writeText(flow.user_code);
        setCopied(true);
        setTimeout(() => setCopied(false), 4000);
      } catch (e) {}

      // Start background polling
      pollDeviceToken(flow.device_code, flow.interval)
        .then((result) => {
          setToken(result.token);
          setUsername(result.username);
          setDeviceFlow(null);
        })
        .catch((pollErr) => {
          console.error("Poll error:", pollErr);
          setError(pollErr.message || "Authorization failed.");
          setDeviceFlow(null);
        });

    } catch (err) {
      console.error(err);
      setError(err.message || "Failed to start GitHub authorization.");
      setIsLoggingIn(false);
    }
  };

  const copyUserCode = () => {
    if (deviceFlow?.user_code) {
      navigator.clipboard.writeText(deviceFlow.user_code);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const openGitHubVerification = () => {
    copyUserCode();
    window.open(deviceFlow?.verification_uri || 'https://github.com/login/device', '_blank');
  };

  const handlePush = async () => {
    if (!token) {
      await startGitHubAuth();
      return;
    }

    setIsPushing(true);
    setError('');
    setStatusMessage('Connecting to GitHub...');

    try {
      const repo = await pushProjectToGitHub({
        project,
        repoName: repoName.trim(),
        isPrivate,
        token,
        onStatusUpdate: (msg) => setStatusMessage(msg)
      });

      setCreatedRepo(repo);
      if (onProjectUpdated) {
        onProjectUpdated({ ...project, githubRepo: repo.html_url });
      }
    } catch (err) {
      console.error("Push error:", err);
      setError(err.message || "Failed to push to GitHub.");
    } finally {
      setIsPushing(false);
    }
  };

  const handleDisconnect = () => {
    sessionStorage.removeItem('github_access_token');
    sessionStorage.removeItem('github_username');
    setToken(null);
    setUsername(null);
    setDeviceFlow(null);
  };

  return (
    <AnimatePresence>
      <div className="modal-overlay" onClick={onClose}>
        <motion.div 
          className="github-modal glass-panel"
          onClick={(e) => e.stopPropagation()}
          initial={{ opacity: 0, scale: 0.95, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 20 }}
          transition={{ duration: 0.2 }}
        >
          {/* Modal Header */}
          <div className="modal-header">
            <div className="header-left">
              <div className="github-icon-badge">
                <GithubIcon size={24} />
              </div>
              <div>
                <h2>Export to GitHub</h2>
                <p className="modal-sub">Push all code & schemas directly to your personal GitHub profile.</p>
              </div>
            </div>
            <button className="close-btn" onClick={onClose}>
              <X size={20} />
            </button>
          </div>

          {/* Modal Body */}
          <div className="modal-body">
            {error && (
              <div className="alert-box error">
                <AlertCircle size={18} />
                <span>{error}</span>
              </div>
            )}

            {createdRepo && !isPushing ? (
              <div className="success-screen">
                <div className="success-icon-wrap">
                  <CheckCircle2 size={48} color="#10B981" />
                </div>
                <h3>Repository Ready!</h3>
                <p>Your code has been successfully pushed and is live on GitHub.</p>
                <a 
                  href={createdRepo.html_url} 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="repo-link-btn"
                >
                  <GithubIcon size={18} />
                  <span>Open {createdRepo.html_url.replace('https://github.com/', '')}</span>
                  <ExternalLink size={16} />
                </a>

                <div className="modal-actions" style={{ marginTop: '1.5rem' }}>
                  <button className="secondary-btn" onClick={() => setCreatedRepo(null)}>
                    Push Again
                  </button>
                  <button className="glow-btn" onClick={onClose}>
                    Done
                  </button>
                </div>
              </div>
            ) : (
              <>
                {/* GitHub Authentication State */}
                <div className="auth-status-panel">
                  {token ? (
                    <div className="connected-status">
                      <div className="user-info">
                        <div className="green-dot"></div>
                        <span>Connected to GitHub as <strong>{username || 'GitHub User'}</strong></span>
                      </div>
                      <button 
                        className="switch-account-btn" 
                        onClick={handleDisconnect}
                        disabled={isPushing}
                      >
                        Disconnect
                      </button>
                    </div>
                  ) : deviceFlow ? (
                    <div className="device-flow-box">
                      <p className="device-instruction">
                        1. Copy your authorization code:
                      </p>
                      <div className="code-display" onClick={copyUserCode}>
                        <span className="code-text">{deviceFlow.user_code}</span>
                        <button className="copy-code-btn" type="button">
                          {copied ? <Check size={16} color="#10B981" /> : <Copy size={16} />}
                          <span>{copied ? 'Copied!' : 'Copy'}</span>
                        </button>
                      </div>

                      <p className="device-instruction" style={{ marginTop: '0.75rem' }}>
                        2. Click below to log in and authorize on GitHub:
                      </p>
                      <button 
                        type="button"
                        className="open-github-btn"
                        onClick={openGitHubVerification}
                      >
                        <GithubIcon size={18} />
                        <span>Open GitHub & Authorize</span>
                        <ExternalLink size={16} />
                      </button>

                      <div className="waiting-indicator">
                        <Loader2 size={16} className="spin-icon" />
                        <span>Waiting for approval on GitHub...</span>
                      </div>
                    </div>
                  ) : (
                    <div className="login-prompt">
                      <p>Sign in with your GitHub account to authorize repository creation (no personal token needed).</p>
                      <button 
                        className="github-login-btn"
                        onClick={startGitHubAuth}
                        disabled={isLoggingIn}
                      >
                        {isLoggingIn ? (
                          <>
                            <Loader2 size={18} className="spin-icon" />
                            <span>Connecting...</span>
                          </>
                        ) : (
                          <>
                            <GithubIcon size={18} />
                            <span>Sign in with GitHub</span>
                          </>
                        )}
                      </button>
                    </div>
                  )}
                </div>

                {/* Repository Configuration */}
                <div className="form-group">
                  <label>Repository Name</label>
                  <input 
                    type="text" 
                    value={repoName} 
                    onChange={(e) => setRepoName(e.target.value)}
                    placeholder="my-ai-app"
                    disabled={isPushing}
                  />
                  <span className="field-hint">Will be created under your GitHub account.</span>
                </div>

                <div className="form-group">
                  <label>Visibility</label>
                  <div className="visibility-options">
                    <button 
                      type="button"
                      className={`visibility-btn ${!isPrivate ? 'active' : ''}`}
                      onClick={() => setIsPrivate(false)}
                      disabled={isPushing}
                    >
                      <Globe size={18} />
                      <div>
                        <strong>Public</strong>
                        <span>Anyone on internet can see</span>
                      </div>
                    </button>

                    <button 
                      type="button"
                      className={`visibility-btn ${isPrivate ? 'active' : ''}`}
                      onClick={() => setIsPrivate(true)}
                      disabled={isPushing}
                    >
                      <Lock size={18} />
                      <div>
                        <strong>Private</strong>
                        <span>Only you can access</span>
                      </div>
                    </button>
                  </div>
                </div>

                {/* Push progress */}
                {isPushing && (
                  <div className="push-progress">
                    <Loader2 size={20} className="spin-icon" />
                    <span>{statusMessage}</span>
                  </div>
                )}

                {/* Footer Buttons */}
                <div className="modal-actions">
                  <button className="secondary-btn" onClick={onClose} disabled={isPushing}>
                    Cancel
                  </button>
                  <button 
                    className="glow-btn push-btn" 
                    onClick={handlePush}
                    disabled={isPushing || !repoName.trim() || (!token && !!deviceFlow)}
                  >
                    {isPushing ? (
                      <>
                        <Loader2 size={18} className="spin-icon" />
                        <span>Pushing Code...</span>
                      </>
                    ) : (
                      <>
                        <GithubIcon size={18} />
                        <span>{token ? 'Create & Push Repo' : 'Login with GitHub & Push'}</span>
                        <ArrowRight size={16} />
                      </>
                    )}
                  </button>
                </div>
              </>
            )}
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};

export default GitHubModal;
