import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ShoppingCart, GraduationCap, MessageSquare, Activity, Newspaper, Music, ChevronRight } from 'lucide-react';
import { generateApp } from '../lib/generate';
import LoadingOverlay from '../components/LoadingOverlay';

const Templates = ({ user }) => {
  const navigate = useNavigate();
  const [generatingId, setGeneratingId] = useState(null);
  const templates = [
    { id: 1, icon: <ShoppingCart size={28} />, name: 'E-Commerce', desc: 'Full store with cart & payments', color: '#3B82F6' },
    { id: 2, icon: <GraduationCap size={28} />, name: 'LMS', desc: 'Courses, students, assignments', color: '#8B5CF6' },
    { id: 3, icon: <MessageSquare size={28} />, name: 'Chat App', desc: 'Real-time messaging platform', color: '#10B981' },
    { id: 4, icon: <Activity size={28} />, name: 'Health Tracker', desc: 'Fitness & wellness tracking', color: '#F43F5E' },
    { id: 5, icon: <Newspaper size={28} />, name: 'News App', desc: 'Article feed with categories', color: '#F59E0B' },
    { id: 6, icon: <Music size={28} />, name: 'Music Player', desc: 'Stream and manage music', color: '#EC4899' },
  ];

  const handleTemplateClick = async (template) => {
    if (!user?.uid) {
      alert("Please login first");
      return;
    }
    
    setGeneratingId(template.id);
    try {
      const newProject = await generateApp(`Build a ${template.name} application: ${template.desc}`, user);
      navigate(`/project/${newProject.id}`);
    } catch (error) {
      alert(`Template Generation Failed: ${error.message || error}`);
      setGeneratingId(null);
    }
  };

  return (
    <div className="dashboard-container">
      <LoadingOverlay isVisible={generatingId !== null} />
      <header className="dashboard-header">
        <motion.h1 
          className="text-gradient"
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
        >
          Templates
        </motion.h1>
        <p className="subtitle">Start your project instantly with these pre-built templates.</p>
      </header>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem' }}>
        {templates.map((t, i) => (
          <motion.div
            key={t.id}
            className="glass-panel"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.1 }}
            whileHover={{ scale: 1.03, y: -5, boxShadow: `0 10px 30px ${t.color}33` }}
            style={{ 
              padding: '1.5rem', 
              display: 'flex', 
              alignItems: 'center', 
              gap: '1rem',
              cursor: generatingId ? 'not-allowed' : 'pointer',
              opacity: generatingId && generatingId !== t.id ? 0.5 : 1
            }}
            onClick={() => !generatingId && handleTemplateClick(t)}
          >
            <div style={{ 
              width: '56px', height: '56px', 
              borderRadius: '16px', 
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              background: `linear-gradient(135deg, ${t.color}cc, ${t.color})`,
              color: 'white',
              boxShadow: `0 8px 20px ${t.color}66`
            }}>
              {t.icon}
            </div>
            
            <div style={{ flex: 1 }}>
              <h3 style={{ fontSize: '1.1rem', marginBottom: '4px' }}>{t.name}</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                {generatingId === t.id ? 'Generating code via AI...' : t.desc}
              </p>
            </div>

            {generatingId === t.id ? (
              <div className="spinner" style={{ width: 16, height: 16 }}></div>
            ) : (
              <ChevronRight color="var(--text-muted)" />
            )}
          </motion.div>
        ))}
      </div>
    </div>
  );
};

export default Templates;
