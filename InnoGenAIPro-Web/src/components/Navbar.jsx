import { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Zap, LayoutTemplate, FolderGit2, LogOut } from 'lucide-react';
import { auth, db } from '../lib/firebase';
import { signOut } from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import './Navbar.css';

const Navbar = ({ user }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const [firestoreName, setFirestoreName] = useState('');

  useEffect(() => {
    const fetchName = async () => {
      if (!user?.uid) return;
      try {
        const userDoc = await getDoc(doc(db, 'users', user.uid));
        if (userDoc.exists() && userDoc.data().name) {
          setFirestoreName(userDoc.data().name);
        }
      } catch (err) {
        console.error("Error fetching name", err);
      }
    };
    fetchName();
  }, [user]);

  const handleLogout = async () => {
    try {
      await signOut(auth);
      navigate('/login');
    } catch (error) {
      console.error("Failed to log out", error);
    }
  };

  const links = [
    { name: 'Dashboard', path: '/dashboard', icon: <Zap size={18} /> },
    { name: 'Templates', path: '/templates', icon: <LayoutTemplate size={18} /> },
  ];

  return (
    <nav className="navbar glass-panel">
      <div className="nav-brand">
        <div className="logo-icon">
          <Zap size={24} color="#fff" fill="#fff" />
        </div>
        <span className="brand-text text-gradient">InnoGen AI Pro</span>
      </div>

      <div className="nav-links">
        {links.map((link) => (
          <Link
            key={link.path}
            to={link.path}
            className={`nav-link ${location.pathname === link.path ? 'active' : ''}`}
          >
            {link.icon}
            <span>{link.name}</span>
            {location.pathname === link.path && (
              <motion.div
                layoutId="nav-indicator"
                className="nav-indicator"
                initial={false}
                transition={{ type: "spring", stiffness: 300, damping: 30 }}
              />
            )}
          </Link>
        ))}
      </div>

      <div className="nav-profile">
        <Link to="/profile" className="avatar" title="View Profile">
          {firestoreName ? firestoreName.charAt(0).toUpperCase() : (user?.displayName ? user.displayName.charAt(0).toUpperCase() : user?.email?.charAt(0).toUpperCase())}
        </Link>
        <button className="logout-btn" onClick={handleLogout} title="Log Out">
          <LogOut size={18} />
        </button>
      </div>
    </nav>
  );
};

export default Navbar;
