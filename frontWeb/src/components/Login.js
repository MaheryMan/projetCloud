import React, { useState } from 'react';
import { auth, googleProvider } from '../firebase';
import { signInWithPopup } from 'firebase/auth';
import { useNavigate } from 'react-router-dom';
import './Login.css';

function Login({ onLogin }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      });

      if (!response.ok) {
        throw new Error('Email ou mot de passe incorrect');
      }

      const data = await response.json();
      console.log('Connexion réussie:', data);
      console.log('data.roles =', data.roles);
      console.log('Type:', typeof data.roles);
      console.log('Is Array?', Array.isArray(data.roles));
      console.log('First element:', data.roles ? data.roles[0] : 'undefined');
      console.log('Includes Manager?', data.roles ? data.roles.includes('Manager') : 'no roles');
      
      localStorage.setItem('token', data.token);
      if (onLogin) onLogin(data);
      
      // Redirection basée sur le rôle
      if (data.roles && data.roles.includes('Manager')) {
        console.log('REDIRECTION VERS DASHBOARD');
        navigate('/dashboard');
      } else {
        console.log('REDIRECTION VERS MAP');
        navigate('/');
      }
      
    } catch (err) {
      setError(err.message || 'Une erreur est survenue');
    } finally {
      setLoading(false);
    }
  };

  // Connexion via Google
  const handleGoogleLogin = async () => {
    setError('');
    setLoading(true);
    try {
      const result = await signInWithPopup(auth, googleProvider);
      const user = result.user;
      const idToken = await user.getIdToken();
      // Appel backend pour login/register Google
      const response = await fetch('http://localhost:8080/api/auth/register-google', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idToken })
      });
      if (!response.ok) {
        throw new Error('Erreur lors de la connexion Google');
      }
      const data = await response.json();
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data.user.id));
      if (onLogin) onLogin(data);
      if (data.user.roles && data.user.roles.some(role => role.libelle === 'Manager')) {
        navigate('/dashboard');
      } else {
        navigate('/');
      }
    } catch (err) {
      setError(err.message || 'Erreur Google Auth');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-wrapper">
        {/* Left Side - Visual Section */}
        <div className="login-visual">
          <div className="visual-content">
            <div className="visual-icon"></div>
            <h2 className="visual-title">Travaux Routiers</h2>
            <p className="visual-description">
              Plateforme de gestion et de signalement des travaux routiers à Antananarivo
            </p>
            <div className="visual-features">
              <div className="feature-item">Suivi en temps réel</div>
              <div className="feature-item">Gestion centralisée</div>
              <div className="feature-item">Rapports détaillés</div>
            </div>
          </div>
        </div>

        {/* Right Side - Form Section */}
        <div className="login-card">
          <div className="login-header">
            <h1 className="login-title">Connexion</h1>
            <p className="login-subtitle">Bienvenue ! Connectez-vous à votre compte</p>
          </div>
          
          <form onSubmit={handleSubmit} className="login-form">
            {error && <div className="error-message">{error}</div>}
            <div className="form-group">
              <input
                type="email"
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="votre@email.com"
                required
                disabled={loading}
              />
              <label htmlFor="email">Email</label>
            </div>
            <div className="form-group">
              <input
                type="password"
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                required
                disabled={loading}
              />
              <label htmlFor="password">Mot de passe</label>
            </div>
            <div className="form-options">
              <label className="remember-me">
                <input type="checkbox" />
                <span>Se souvenir de moi</span>
              </label>
              <a href="#forgot" className="forgot-password">Mot de passe oublié ?</a>
            </div>
            <button type="submit" className="login-button" disabled={loading}>
              {loading ? 'Connexion...' : 'Se connecter'}
            </button>
          </form>
          {/* <div style={{ textAlign: 'center', marginTop: 16 }}>
            <button type="button" className="login-google-btn" onClick={handleGoogleLogin} disabled={loading}>
              <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" alt="Google" style={{ width: 22, height: 22, marginRight: 10, background: 'none', verticalAlign: 'middle' }} />
              Se connecter avec Google
            </button>
          </div> */}

          {/* <div className="signup-link">
            Pas encore de compte ? <a href="/register">S'inscrire</a>
          </div> */}
        </div>
      </div>
    </div>
  );
}

export default Login;