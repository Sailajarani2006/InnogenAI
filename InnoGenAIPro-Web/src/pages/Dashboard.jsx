import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Mic, Send, Sparkles, LayoutGrid, FolderOpen, Trash2 } from 'lucide-react';
import GithubIcon from '../components/GithubIcon';
import { supabase, isSupabaseConfigured } from '../lib/supabase';
import { db } from '../lib/firebase';
import { collection, query, where, onSnapshot, doc, getDoc, deleteDoc } from 'firebase/firestore';
import { generateApp } from '../lib/generate';
import LoadingOverlay from '../components/LoadingOverlay';
import './Dashboard.css';

const normalizeProject = (row) => ({
  ...row,
  title: row.title || row.name || 'Untitled',
  createdAt: row.created_at || row.createdAt || Date.now(),
  updatedAt: row.updated_at || row.updatedAt || Date.now(),
  userId: row.user_id || row.userId || '',
  userEmail: row.user_email || row.userEmail || '',
  githubRepo: row.github_repo || row.githubRepo || '',
  hasCode: row.has_code ?? row.hasCode ?? true,
  generatedCode: row.generated_code || row.generatedCode || {},
  techStack: row.tech_stack || row.techStack || {}
});

const Dashboard = ({ user }) => {
  const navigate = useNavigate();
  const [idea, setIdea] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [recentProjects, setRecentProjects] = useState([]);
  const [loadingProjects, setLoadingProjects] = useState(true);
  const [firestoreName, setFirestoreName] = useState('');

  const quickActions = [
    { icon: <Sparkles />, label: 'Generate App', color: 'purple' },
    { icon: <LayoutGrid />, label: 'Templates', color: 'blue' },
    { icon: <FolderOpen />, label: 'My Projects', color: 'cyan' },
  ];

  const suggestions = [
    '📱 Social media app',
    '🛒 E-commerce platform',
    '🎓 Learning platform',
    '🏥 Health tracker'
  ];

  useEffect(() => {
    if (!user?.uid) {
      setLoadingProjects(false);
      return;
    }

    // Fetch user display name from Firestore if synced with mobile app
    getDoc(doc(db, 'users', user.uid))
      .then((userDoc) => {
        if (userDoc.exists() && userDoc.data()?.name) {
          setFirestoreName(userDoc.data().name);
        }
      })
      .catch((err) => console.warn("Could not fetch user name:", err));

    const getSortTime = (t) => {
      if (!t) return 0;
      if (typeof t === 'number') return t;
      if (t.toMillis) return t.toMillis();
      if (t.seconds) return t.seconds * 1000;
      if (t instanceof Date) return t.getTime();
      return 0;
    };

    // ── 1. Supabase Data & Real-Time Sync ──
    let supabaseChannel = null;
    if (isSupabaseConfigured() && supabase) {
      supabase
        .from('projects')
        .select('*')
        .eq('user_id', user.uid)
        .order('created_at', { ascending: false })
        .then(({ data, error }) => {
          if (!error && data && data.length > 0) {
            setRecentProjects(data.map(normalizeProject));
            setLoadingProjects(false);
          }
        });

      supabaseChannel = supabase
        .channel('realtime_dashboard_projects')
        .on(
          'postgres_changes',
          { event: '*', schema: 'public', table: 'projects', filter: `user_id=eq.${user.uid}` },
          (payload) => {
            if (payload.eventType === 'INSERT') {
              const item = normalizeProject(payload.new);
              setRecentProjects(prev => [item, ...prev.filter(p => p.id !== item.id)]);
            } else if (payload.eventType === 'UPDATE') {
              const item = normalizeProject(payload.new);
              setRecentProjects(prev => prev.map(p => p.id === item.id ? item : p));
            } else if (payload.eventType === 'DELETE') {
              setRecentProjects(prev => prev.filter(p => p.id !== payload.old.id));
            }
          }
        )
        .subscribe();
    }

    // ── 2. Firestore Fallback / Dual Real-Time Sync ──
    const qUid = query(
      collection(db, 'projects'),
      where('userId', '==', user.uid)
    );

    const unsubUid = onSnapshot(
      qUid,
      (querySnapshot) => {
        if (!isSupabaseConfigured() || !supabase) {
          const list = querySnapshot.docs.map(d => normalizeProject({ id: d.id, ...d.data() }));
          list.sort((a, b) => getSortTime(b.createdAt) - getSortTime(a.createdAt));
          setRecentProjects(list);
          setLoadingProjects(false);
        }
      },
      (error) => {
        console.warn("Firestore projects listener status:", error?.message);
        setLoadingProjects(false);
      }
    );

    return () => {
      unsubUid();
      if (supabaseChannel && supabase) {
        supabase.removeChannel(supabaseChannel);
      }
    };
  }, [user]);

  const handleDeleteProject = async (e, projectId) => {
    e.stopPropagation();
    if (!window.confirm("Are you sure you want to delete this project?")) return;

    // Optimistically remove immediately from UI so it instantly vanishes from Recent
    setRecentProjects(prev => prev.filter(p => p.id !== projectId));

    try {
      if (isSupabaseConfigured() && supabase) {
        await supabase.from('projects').delete().eq('id', projectId);
      }
      await deleteDoc(doc(db, 'projects', projectId));
    } catch (error) {
      console.error("Failed to delete project:", error);
    }
  };

  const handleGenerate = async () => {
    if (!idea || !user?.uid) return;
    setIsGenerating(true);
    
    try {
      const newProject = await generateApp(idea, user);
      setIdea('');
      setIsGenerating(false);
      navigate(`/project/${newProject.id}`);
    } catch (error) {
      alert(`Generation Failed: ${error.message || error}`);
      setIsGenerating(false);
    }
  };

  return (
    <div className="dashboard-container">
      <LoadingOverlay isVisible={isGenerating} />
      {/* Header section with gradient text */}
      <header className="dashboard-header">
        <motion.div 
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="header-content"
        >
          <h1 className="text-gradient">Hello, {firestoreName || user?.displayName || user?.email?.split('@')[0]} 👋</h1>
          <p className="subtitle">What will you build today?</p>
        </motion.div>

        {/* Floating Glowing Input */}
        <motion.div 
          className={`idea-input-wrapper ${isGenerating ? 'generating' : ''}`}
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.1 }}
        >
          <div className="glass-panel input-container">
            <input 
              type="text" 
              placeholder="Describe your app idea..." 
              value={idea}
              onChange={(e) => setIdea(e.target.value)}
              disabled={isGenerating}
            />
            <button className="icon-btn" disabled={isGenerating}><Mic size={20} /></button>
            <button className="send-btn" onClick={handleGenerate} disabled={isGenerating || !idea}>
              {isGenerating ? <div className="spinner"></div> : <Send size={18} />}
            </button>
          </div>
          {isGenerating && (
            <div className="scanning-line"></div>
          )}
        </motion.div>
      </header>

      {/* Quick Actions */}
      <section className="quick-actions">
        {quickActions.map((action, i) => (
          <motion.div
            key={action.label}
            className="action-card glass-panel"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 + (i * 0.1) }}
            whileHover={{ y: -5, scale: 1.02 }}
            style={{ cursor: 'pointer' }}
            onClick={() => {
              if (action.label === 'Templates') {
                navigate('/templates');
              } else if (action.label === 'My Projects') {
                navigate('/projects');
              } else if (action.label === 'Generate App') {
                window.scrollTo({ top: 0, behavior: 'smooth' });
                document.querySelector('.input-container input')?.focus();
              }
            }}
          >
            <div className={`action-icon bg-${action.color}`}>
              {action.icon}
            </div>
            <span>{action.label}</span>
          </motion.div>
        ))}
      </section>

      {/* Try these ideas */}
      <section className="suggestions">
        <h3 className="section-title">Try These Ideas</h3>
        <div className="chips-row">
          {suggestions.map((sug, i) => (
            <motion.button
              key={i}
              className="suggestion-chip"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.4 + (i * 0.05) }}
              whileHover={{ scale: 1.05 }}
              onClick={() => setIdea(sug.replace(/^[^\s]+\s/, ''))}
            >
              {sug}
            </motion.button>
          ))}
        </div>
      </section>

      {/* Recent Projects */}
      <section className="recent-projects">
        <div className="section-header">
          <h3 className="section-title">Recent Projects</h3>
          <span className="count">{recentProjects.length} projects</span>
        </div>
        
        <div className="projects-grid">
          {loadingProjects ? (
            <div style={{ color: 'var(--text-muted)' }}>Loading projects...</div>
          ) : recentProjects.length === 0 ? (
            <div style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>
              No projects yet. Start generating!
            </div>
          ) : (
            recentProjects.map((project, i) => (
              <motion.div
                key={project.id}
                className="project-card glass-panel"
                onClick={() => navigate(`/project/${project.id}`)}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: 0.5 + (i * 0.1) }}
                whileHover={{ 
                  y: -10, 
                  rotateX: 5,
                  rotateY: -5,
                  boxShadow: '0 20px 40px rgba(124, 58, 237, 0.2)'
                }}
                style={{ perspective: 1000, cursor: 'pointer' }}
              >
                <div className="project-top">
                  <div className="project-icon">
                    <Sparkles size={20} color="#fff" />
                  </div>
                  <div className="project-info">
                    <h4>{project.name || project.title || project.idea?.substring(0, 20) || project.prompt?.substring(0, 20) || 'Untitled'}</h4>
                    <span>
                      {project.createdAt?.toDate 
                        ? project.createdAt.toDate().toLocaleDateString() 
                        : (typeof project.createdAt === 'number' 
                           ? new Date(project.createdAt).toLocaleDateString() 
                           : 'Just now')}
                    </span>
                  </div>
                  <div className="badge">Complete</div>
                  {project.githubRepo && (
                    <a
                      href={project.githubRepo}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="github-badge"
                      title="Open on GitHub"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <GithubIcon size={15} />
                    </a>
                  )}
                  <button 
                    className="del-btn" 
                    title="Delete project"
                    onClick={(e) => handleDeleteProject(e, project.id)}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
                <p className="project-desc">{project.description || project.idea || project.prompt}</p>
                <div className="project-tags">
                  {(project.tags || (project.techStack ? Object.values(project.techStack) : ['React', 'Node.js'])).map((tag, idx) => (
                    tag ? <span key={idx} className="tag">{tag}</span> : null
                  ))}
                </div>
              </motion.div>
            ))
          )}
        </div>
      </section>
    </div>
  );
};

export default Dashboard;
