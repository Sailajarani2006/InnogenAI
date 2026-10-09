import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { doc, getDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { motion } from 'framer-motion';
import { ArrowLeft, Code, FileText, Database, Server, ExternalLink, UploadCloud } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import GitHubModal from '../components/GitHubModal';
import GithubIcon from '../components/GithubIcon';
import './ProjectView.css';

const ProjectView = ({ user }) => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('readme');
  const [isGitHubModalOpen, setIsGitHubModalOpen] = useState(false);

  useEffect(() => {
    const fetchProject = async () => {
      try {
        const docRef = doc(db, 'projects', id);
        const docSnap = await getDoc(docRef);

        if (docSnap.exists() && (docSnap.data().userId === user.uid || (user.email && docSnap.data().userEmail === user.email) || !docSnap.data().userId)) {
          setProject({ id: docSnap.id, ...docSnap.data() });
        } else {
          alert('Project not found.');
          navigate('/dashboard');
        }
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchProject();
  }, [id, user, navigate]);

  if (loading) {
    return <div className="loading-state"><div className="spinner"></div></div>;
  }

  if (!project) return null;

  let codeData = {};
  if (typeof project.generatedCode === 'string') {
    try { codeData = JSON.parse(project.generatedCode); } catch (e) { codeData = {}; }
  } else if (project.generatedCode) {
    codeData = project.generatedCode;
  }

  const tabs = [
    { id: 'readme', label: 'README.md', icon: <FileText size={18} /> },
    { id: 'frontend', label: 'Frontend Code', icon: <Code size={18} /> },
    { id: 'backend', label: 'Backend Code', icon: <Server size={18} /> },
    { id: 'database', label: 'Database Schema', icon: <Database size={18} /> },
  ];

  const renderContent = () => {
    switch (activeTab) {
      case 'readme':
        return (
          <div className="markdown-body">
            <ReactMarkdown>{codeData.readme || '*No README generated.*'}</ReactMarkdown>
          </div>
        );
      case 'frontend':
        return (
          <pre className="code-block">
            <code>{codeData.frontendCode || '// No frontend code generated.'}</code>
          </pre>
        );
      case 'backend':
        return (
          <pre className="code-block">
            <code>{codeData.backendCode || '// No backend code generated.'}</code>
          </pre>
        );
      case 'database':
        return (
          <pre className="code-block">
            <code>{codeData.databaseSchema || '-- No schema generated.'}</code>
          </pre>
        );
      default:
        return null;
    }
  };

  return (
    <div className="project-view-container">
      <GitHubModal 
        isOpen={isGitHubModalOpen}
        onClose={() => setIsGitHubModalOpen(false)}
        project={project}
        onProjectUpdated={(updatedProject) => setProject(updatedProject)}
      />

      <header className="project-header">
        <button className="back-btn" onClick={() => navigate('/dashboard')}>
          <ArrowLeft size={20} /> Back to Dashboard
        </button>

        <div className="project-header-main">
          <div className="project-title-area">
            <h1 className="text-gradient">{project.name || project.title || project.idea}</h1>
            <p className="subtitle">{project.description || project.prompt || project.idea}</p>
          </div>

          <div className="project-header-actions">
            {project.githubRepo ? (
              <div className="github-actions-group">
                <a 
                  href={project.githubRepo} 
                  target="_blank" 
                  rel="noopener noreferrer" 
                  className="view-github-btn"
                >
                  <GithubIcon size={18} />
                  <span>View on GitHub</span>
                  <ExternalLink size={14} />
                </a>

                <button 
                  className="glow-btn push-code-btn"
                  onClick={() => setIsGitHubModalOpen(true)}
                >
                  <UploadCloud size={18} />
                  <span>Update Repository</span>
                </button>
              </div>
            ) : (
              <button 
                className="glow-btn push-code-btn"
                onClick={() => setIsGitHubModalOpen(true)}
              >
                <GithubIcon size={18} />
                <span>Push to GitHub</span>
              </button>
            )}
          </div>
        </div>
      </header>

      <div className="project-workspace">
        <div className="tabs-sidebar glass-panel">
          {tabs.map(tab => (
            <button
              key={tab.id}
              className={`tab-btn ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.icon}
              <span>{tab.label}</span>
            </button>
          ))}
        </div>

        <motion.div 
          className="tab-content glass-panel"
          key={activeTab}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.2 }}
        >
          {renderContent()}
        </motion.div>
      </div>
    </div>
  );
};

export default ProjectView;
