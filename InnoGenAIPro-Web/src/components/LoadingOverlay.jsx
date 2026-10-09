import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Check, Loader2 } from 'lucide-react';
import '../pages/LoadingOverlay.css';

const LoadingOverlay = ({ isVisible, currentStepOverride }) => {
  const steps = [
    "Analyzing app requirements...",
    "Designing frontend UI (HTML, CSS, JS)...",
    "Writing FastAPI/Flask Python backend...",
    "Creating SQL Database schema...",
    "Assembling code workspace..."
  ];

  const [activeStep, setActiveStep] = useState(0);

  useEffect(() => {
    if (!isVisible) {
      setActiveStep(0);
      return;
    }

    // Auto-progress simulated steps up to the 4th step (index 3)
    const interval = setInterval(() => {
      setActiveStep((prev) => {
        if (prev < 3) {
          return prev + 1;
        }
        return prev;
      });
    }, 4500); // 4.5 seconds per step

    return () => clearInterval(interval);
  }, [isVisible]);

  // If we receive an override (e.g. from the backend finishing), use it
  const currentStep = currentStepOverride !== undefined ? currentStepOverride : activeStep;

  if (!isVisible) return null;

  return (
    <div className="loading-overlay">
      <motion.div 
        className="loading-card glass-panel"
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.9 }}
      >
        <div className="loading-spinner-large">
          <div className="spinner-ring"></div>
          <div className="spinner-ring"></div>
          <div className="spinner-ring"></div>
        </div>

        <div className="loading-title text-gradient">Generating Your App</div>

        <div className="loading-steps-list">
          {steps.map((step, idx) => {
            let statusClass = "pending";
            let icon = <span style={{ fontSize: '0.8rem' }}>{idx + 1}</span>;

            if (idx < currentStep) {
              statusClass = "completed";
              icon = <Check size={14} />;
            } else if (idx === currentStep) {
              statusClass = "active";
              icon = <Loader2 size={14} className="spinner" />;
            }

            return (
              <div key={idx} className={`loading-step-item ${statusClass}`}>
                <div className="step-indicator">
                  {icon}
                </div>
                <span>{step}</span>
              </div>
            );
          })}
        </div>
      </motion.div>
    </div>
  );
};

export default LoadingOverlay;
