import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { motion, useMotionValue, useTransform } from 'framer-motion';
import { Zap, Mail, Lock, User, AlertCircle } from 'lucide-react';
import { auth } from '../lib/firebase';
import { signInWithEmailAndPassword, createUserWithEmailAndPassword, updateProfile, sendPasswordResetEmail } from 'firebase/auth';
import './Auth.css';

const Auth = ({ defaultIsLogin = true }) => {
  const [isLogin, setIsLogin] = useState(defaultIsLogin);
  const [isForgotPassword, setIsForgotPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  // 3D Tilt effect logic
  const x = useMotionValue(0);
  const y = useMotionValue(0);
  const rotateX = useTransform(y, [-300, 300], [15, -15]);
  const rotateY = useTransform(x, [-300, 300], [-15, 15]);

  const handleMouseMove = (event) => {
    const rect = event.currentTarget.getBoundingClientRect();
    x.set(event.clientX - rect.left - rect.width / 2);
    y.set(event.clientY - rect.top - rect.height / 2);
  };

  const handleMouseLeave = () => {
    x.set(0);
    y.set(0);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const trimmedEmail = email.trim();
      if (isForgotPassword) {
        await sendPasswordResetEmail(auth, trimmedEmail);
        setSuccessMessage('Password reset link sent to your email!');
      } else if (isLogin) {
        await signInWithEmailAndPassword(auth, trimmedEmail, password);
        navigate('/dashboard');
      } else {
        const userCredential = await createUserWithEmailAndPassword(auth, trimmedEmail, password);
        await updateProfile(userCredential.user, { displayName: name });
        navigate('/dashboard');
      }
    } catch (err) {
      console.error(err);
      if (err.code === 'auth/invalid-credential' || err.code === 'auth/wrong-password') {
        setError('Invalid email or password. Please check your spelling.');
      } else if (err.code === 'auth/user-not-found') {
        setError('No account found with this email. Please sign up first.');
      } else if (err.code === 'auth/email-already-in-use') {
        setError('An account with this email already exists.');
      } else if (err.code === 'auth/too-many-requests') {
        setError('Too many failed login attempts. Please try again later.');
      } else {
        setError(`Error: ${err.message}`);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      {/* 3D Showcase Side */}
      <div 
        className="auth-showcase"
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
      >
        <motion.div 
          className="mockup-container"
          style={{ rotateX, rotateY, perspective: 1000 }}
          animate={{ y: [0, -10, 0] }}
          transition={{ repeat: Infinity, duration: 4, ease: "easeInOut" }}
        >
          <div className="glass-panel mockup">
            <div className="mockup-header">
              <Zap size={24} color="var(--primary)" />
              <div className="mockup-lines">
                <div className="line short"></div>
                <div className="line long"></div>
              </div>
            </div>
            <div className="mockup-body">
              <div className="mockup-card"></div>
              <div className="mockup-card"></div>
              <div className="mockup-card highlight"></div>
            </div>
          </div>
        </motion.div>
        
        <div className="showcase-text">
          <h1 className="text-gradient">Turn Ideas into Apps Instantly</h1>
          <p>The ultimate concept-to-code AI engine.</p>
        </div>
      </div>

      {/* Auth Form Side */}
      <div className="auth-form-wrapper">
        <motion.div 
          className="glass-panel auth-card"
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5 }}
          key={isLogin ? 'login' : 'register'}
        >
          <div className="auth-header">
            <div className="logo-icon">
              <Zap size={28} color="#fff" fill="#fff" />
            </div>
            <h2>{isForgotPassword ? 'Reset Password' : (isLogin ? 'Welcome Back' : 'Create Account')}</h2>
            <p>{isForgotPassword ? 'Enter your email to receive a reset link.' : (isLogin ? 'Sign in to continue building.' : 'Start generating apps today.')}</p>
          </div>

          <form onSubmit={handleSubmit} className="auth-form">
            {error && (
              <div style={{ backgroundColor: 'rgba(239,68,68,0.1)', color: '#ef4444', padding: '12px', borderRadius: '8px', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.9rem' }}>
                <AlertCircle size={18} />
                {error}
              </div>
            )}

            {successMessage && (
              <div style={{ backgroundColor: 'rgba(16,185,129,0.1)', color: '#10b981', padding: '12px', borderRadius: '8px', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.9rem' }}>
                <AlertCircle size={18} />
                {successMessage}
              </div>
            )}
            
            {!isLogin && (
              <div className="input-group">
                <User size={18} className="input-icon" />
                <input 
                  type="text" 
                  placeholder="Full Name" 
                  required 
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>
            )}
            
            <div className="input-group">
              <Mail size={18} className="input-icon" />
              <input 
                type="email" 
                placeholder="Email address" 
                required 
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            
            {!isForgotPassword && (
              <div className="input-group">
                <Lock size={18} className="input-icon" />
                <input 
                  type="password" 
                  placeholder="Password" 
                  required 
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
            )}

            {isLogin && !isForgotPassword && (
              <div style={{ textAlign: 'right', marginTop: '-10px', marginBottom: '15px' }}>
                <button 
                  type="button" 
                  className="text-btn" 
                  style={{ fontSize: '0.85rem', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
                  onClick={() => { setIsForgotPassword(true); setError(''); setSuccessMessage(''); }}
                >
                  Forgot Password?
                </button>
              </div>
            )}

            <button type="submit" className="glow-btn full-width" disabled={loading}>
              {loading ? 'Processing...' : (isForgotPassword ? 'Send Reset Link' : (isLogin ? 'Sign In' : 'Sign Up'))}
            </button>
          </form>

          <div className="auth-switch">
            {isForgotPassword ? (
              <p>
                Remember your password?{' '}
                <button 
                  className="text-btn"
                  style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
                  onClick={() => { setIsForgotPassword(false); setIsLogin(true); setError(''); setSuccessMessage(''); }}
                >
                  Log in
                </button>
              </p>
            ) : (
              <p>
                {isLogin ? "Don't have an account?" : "Already have an account?"}
                <Link 
                  to={isLogin ? "/register" : "/login"}
                  className="text-btn"
                  onClick={() => { setIsLogin(!isLogin); setIsForgotPassword(false); setError(''); setSuccessMessage(''); }}
                >
                  {isLogin ? 'Sign up' : 'Log in'}
                </Link>
              </p>
            )}
          </div>
        </motion.div>
      </div>
    </div>
  );
};

export default Auth;
