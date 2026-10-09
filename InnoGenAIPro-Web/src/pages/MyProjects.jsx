import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Sparkles, Trash2, ArrowLeft } from 'lucide-react';
import GithubIcon from '../components/GithubIcon';
import { db } from '../lib/firebase';
import { collection, query, where, onSnapshot, doc, deleteDoc } from 'firebase/firestore';

const MyProjects = ({ user }) => {
  const navigate = useNavigate();
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.uid) {
      setLoading(false);
      return;
    }

    const getSortTime = (t) => {
      if (!t) return 0;
      if (typeof t === 'number') return t;
      if (t.toMillis) return t.toMillis();
      if (t.seconds) return t.seconds * 1000;
      if (t instanceof Date) return t.getTime();
      return 0;
    };

    const projectsMap = new Map();

    const updateProjectsList = () => {
      const list = Array.from(projectsMap.values());
      list.sort((a, b) => getSortTime(b.createdAt) - getSortTime(a.createdAt));
      setProjects(list);
      setLoading(false);
    };

    const qUid = query(
      collection(db, 'projects'),
      where('userId', '==', user.uid)
    );

    const unsubUid = onSnapshot(
      qUid,
      (querySnapshot) => {
        querySnapshot.forEach(doc => {
          projectsMap.set(doc.id, { id: doc.id, ...doc.data() });
        });
        updateProjectsList();
      },
      (error) => {
        console.error("Error fetching projects by UID:", error);
        setLoading(false);
      }
    );

    let unsubEmail = null;
    if (user.email) {
      const qEmail = query(
        collection(db, 'projects'),
        where('userEmail', '==', user.email)
      );
      unsubEmail = onSnapshot(
        qEmail,
        (querySnapshot) => {
          querySnapshot.forEach(doc => {
            projectsMap.set(doc.id, { id: doc.id, ...doc.data() });
          });
          updateProjectsList();
        },
        (error) => {
          console.warn("Real-time projects by email listener warning:", error);
        }
      );
    }

    return () => {
      unsubUid();
      if (unsubEmail) unsubEmail();
    };
  }, [user]);

  const handleDelete = async (e, projectId) => {
    e.stopPropagation();
    if (!window.confirm("Are you sure you want to delete this project?")) return;
    try {
      await deleteDoc(doc(db, 'projects', projectId));
    } catch (err) {
      console.error("Failed to delete project:", err);
      alert(`Could not delete project: ${err.message || err}`);
    }
  };

  return (
    <div className="dashboard-container">
      <header className="dashboard-header" style={{ flexDirection: 'row', alignItems: 'center', gap: '1rem', paddingBottom: '1rem' }}>
        <button className="icon-btn" onClick={() => navigate('/dashboard')} style={{ width: '40px', height: '40px' }}>
          <ArrowLeft size={20} />
        </button>
        <div>
          <h1 className="text-gradient">My Projects</h1>
          <p className="subtitle">All your generated AI applications.</p>
        </div>
      </header>

      <section className="recent-projects" style={{ marginTop: '0' }}>
        <div className="section-header">
          <h3 className="section-title">All Projects</h3>
          <span className="count">{projects.length} projects</span>
        </div>
        
        <div className="projects-grid">
          {loading ? (
            <div style={{ color: 'var(--text-muted)' }}>Loading projects...</div>
          ) : projects.length === 0 ? (
            <div style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>
              No projects yet. Start generating!
            </div>
          ) : (
            projects.map((project, i) => (
              <motion.div
                key={project.id}
                className="project-card glass-panel"
                onClick={() => navigate(`/project/${project.id}`)}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: 0.1 + (i * 0.05) }}
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
                    onClick={(e) => handleDelete(e, project.id)}
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

export default MyProjects;
