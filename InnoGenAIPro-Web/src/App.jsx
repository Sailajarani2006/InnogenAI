import { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import { auth } from './lib/firebase';
import { onAuthStateChanged } from 'firebase/auth';
import Auth from './pages/Auth';
import Dashboard from './pages/Dashboard';
import Templates from './pages/Templates';
import ProjectView from './pages/ProjectView';
import Profile from './pages/Profile';
import MyProjects from './pages/MyProjects';
import Navbar from './components/Navbar';
import './index.css';

const RequireAuth = ({ children, user, loading }) => {
  if (loading) {
    return <div style={{ height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div className="spinner"></div>
    </div>;
  }
  
  return user ? (
    <>
      <Navbar user={user} />
      <div className="main-content">
        {children}
      </div>
    </>
  ) : <Navigate to="/login" replace />;
};

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  return (
    <>
      <div className="mesh-bg"></div>
      <Router>
        <AnimatePresence mode="wait">
          <Routes>
            <Route path="/" element={<Navigate to={user ? "/dashboard" : "/login"} replace />} />
            
            <Route path="/login" element={
              user ? <Navigate to="/dashboard" replace /> : <Auth defaultIsLogin={true} />
            } />
            
            <Route path="/register" element={
              user ? <Navigate to="/dashboard" replace /> : <Auth defaultIsLogin={false} />
            } />
            
            <Route path="/dashboard" element={
              <RequireAuth user={user} loading={loading}>
                <Dashboard user={user} />
              </RequireAuth>
            } />
            
            <Route path="/templates" element={
              <RequireAuth user={user} loading={loading}>
                <Templates user={user} />
              </RequireAuth>
            } />

            <Route path="/profile" element={
              <RequireAuth user={user} loading={loading}>
                <Profile user={user} />
              </RequireAuth>
            } />

            <Route path="/projects" element={
              <RequireAuth user={user} loading={loading}>
                <MyProjects user={user} />
              </RequireAuth>
            } />

            <Route path="/project/:id" element={
              <RequireAuth user={user} loading={loading}>
                <ProjectView user={user} />
              </RequireAuth>
            } />
            
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AnimatePresence>
      </Router>
    </>
  );
}

export default App;
