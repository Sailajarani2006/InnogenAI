import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { User, Mail, Calendar, Sparkles, FolderGit2 } from 'lucide-react';
import { db, auth } from '../lib/firebase';
import { collection, query, where, getDocs, doc, getDoc } from 'firebase/firestore';
import { sendPasswordResetEmail } from 'firebase/auth';
import './Profile.css';

const Profile = ({ user }) => {
  const [projectCount, setProjectCount] = useState(0);
  const [firestoreName, setFirestoreName] = useState('');
  const [resetMessage, setResetMessage] = useState('');
  const [resetError, setResetError] = useState('');
  const [sendingReset, setSendingReset] = useState(false);

  useEffect(() => {
    const fetchStats = async () => {
      if (!user?.uid) return;
      try {
        const userDoc = await getDoc(doc(db, 'users', user.uid));
        if (userDoc.exists() && userDoc.data().name) {
          setFirestoreName(userDoc.data().name);
        }

        const q = query(collection(db, 'projects'), where('userId', '==', user.uid));
        const snapshot = await getDocs(q);
        setProjectCount(snapshot.size);
      } catch (err) {
        console.error("Failed to fetch profile stats:", err);
      }
    };
    fetchStats();
  }, [user]);

  const handleChangePassword = async () => {
    if (!user?.email) return;
    setSendingReset(true);
    setResetMessage('');
    setResetError('');
    try {
      await sendPasswordResetEmail(auth, user.email);
      setResetMessage('Password reset link sent to your email!');
    } catch (err) {
      console.error(err);
      setResetError('Failed to send reset link. Please try again.');
    } finally {
      setSendingReset(false);
    }
  };

  if (!user) return null;

  const creationDate = user.metadata?.creationTime 
    ? new Date(user.metadata.creationTime).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })
    : 'Unknown';

  return (
    <div className="profile-container">
      <motion.div 
        className="profile-card glass-panel"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <div className="profile-header">
          <div className="profile-avatar-large">
            {firestoreName ? firestoreName.charAt(0).toUpperCase() : (user.displayName ? user.displayName.charAt(0).toUpperCase() : user.email?.charAt(0).toUpperCase())}
          </div>
          <div className="profile-titles">
            <h1 className="text-gradient">{firestoreName || user.displayName || 'Developer'}</h1>
            <p className="subtitle">Pro Member</p>
          </div>
        </div>

        <div className="profile-stats">
          <div className="stat-box">
            <div className="stat-icon bg-purple"><FolderGit2 size={24} /></div>
            <div className="stat-info">
              <h3>{projectCount}</h3>
              <span>Projects Generated</span>
            </div>
          </div>
          <div className="stat-box">
            <div className="stat-icon bg-blue"><Sparkles size={24} /></div>
            <div className="stat-info">
              <h3>Infinity</h3>
              <span>AI Credits</span>
            </div>
          </div>
        </div>

        <div className="profile-details">
          <h3 className="section-title">Account Details</h3>
          
          <div className="detail-row">
            <User size={18} className="detail-icon" />
            <div className="detail-text">
              <span className="label">Name</span>
              <span className="value">{firestoreName || user.displayName || 'Not Set'}</span>
            </div>
          </div>

          <div className="detail-row">
            <Mail size={18} className="detail-icon" />
            <div className="detail-text">
              <span className="label">Email Address</span>
              <span className="value">{user.email}</span>
            </div>
          </div>

          <div className="detail-row">
            <Calendar size={18} className="detail-icon" />
            <div className="detail-text">
              <span className="label">Member Since</span>
              <span className="value">{creationDate}</span>
            </div>
          </div>
        </div>

        <div className="profile-security" style={{ borderTop: '1px solid rgba(255, 255, 255, 0.05)', paddingTop: '1.5rem' }}>
          <h3 className="section-title">Security</h3>
          <p className="subtitle" style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
            Want to change your password? Click below to send a secure password reset link to your email.
          </p>
          {resetMessage && (
            <div style={{ backgroundColor: 'rgba(16,185,129,0.1)', color: '#10b981', padding: '12px', borderRadius: '8px', marginBottom: '1rem', fontSize: '0.9rem' }}>
              {resetMessage}
            </div>
          )}
          {resetError && (
            <div style={{ backgroundColor: 'rgba(239,68,68,0.1)', color: '#ef4444', padding: '12px', borderRadius: '8px', marginBottom: '1rem', fontSize: '0.9rem' }}>
              {resetError}
            </div>
          )}
          <button 
            onClick={handleChangePassword} 
            disabled={sendingReset}
            className="glow-btn"
            style={{ width: 'auto', padding: '10px 20px', fontSize: '0.9rem' }}
          >
            {sendingReset ? 'Sending...' : 'Send Password Reset Link'}
          </button>
        </div>
      </motion.div>
    </div>
  );
};

export default Profile;
