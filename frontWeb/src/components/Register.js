import React, { useState } from 'react';
import './Register.css';

function Register() {
  const [formData, setFormData] = useState({
    nom: '',
    prenom: '',
    email: '',
    numTel: '',
    password: '',
    confirmPassword: ''
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    // Validation
    if (formData.password !== formData.confirmPassword) {
      setError('Les mots de passe ne correspondent pas');
      return;
    }

    if (formData.password.length < 6) {
      setError('Le mot de passe doit contenir au moins 6 caractères');
      return;
    }

    setLoading(true);

    try {
      const response = await fetch('http://localhost:8080/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          nom: formData.nom,
          prenom: formData.prenom,
          email: formData.email,
          numTel: formData.numTel,
          password: formData.password
        }),
      });

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.message || 'Erreur lors de l\'inscription');
      }

      setSuccess('Inscription réussie ! Vous pouvez maintenant vous connecter.');
      setFormData({
        nom: '',
        prenom: '',
        email: '',
        numTel: '',
        password: '',
        confirmPassword: ''
      });

      // Rediriger vers login après 2 secondes
      setTimeout(() => {
        window.location.href = '/login';
      }, 2000);

    } catch (err) {
      setError(err.message || 'Une erreur est survenue');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="register-container">
      <div className="register-wrapper">
        {/* Left Side - Visual Section */}
        <div className="register-visual">
          <div className="visual-content">
            <div className="visual-icon">🚧</div>
            <h2 className="visual-title">Rejoignez-nous</h2>
            <p className="visual-description">
              Créez votre compte et commencez à gérer les travaux routiers efficacement
            </p>
            <div className="visual-benefits">
              <div className="benefit-item">Accès complet à la plateforme</div>
              <div className="benefit-item">Signalement en temps réel</div>
              <div className="benefit-item">Tableaux de bord personnalisés</div>
              <div className="benefit-item">Support technique 24/7</div>
            </div>
          </div>
        </div>

        {/* Right Side - Form Section */}
        <div className="register-card">
          <div className="register-header">
            <h1 className="register-title">Inscription</h1>
            <p className="register-subtitle">Créez votre compte professionnel</p>
          </div>
          
          <form onSubmit={handleSubmit} className="register-form">
            {error && <div className="error-message">{error}</div>}
            {success && <div className="success-message">{success}</div>}
            
            <div className="form-row">
              <div className="form-group">
                <input
                  type="text"
                  id="nom"
                  name="nom"
                  value={formData.nom}
                  onChange={handleChange}
                  placeholder="Votre nom"
                  required
                  disabled={loading}
                />
                <label htmlFor="nom">Nom *</label>
              </div>

              <div className="form-group">
                <input
                  type="text"
                  id="prenom"
                  name="prenom"
                  value={formData.prenom}
                  onChange={handleChange}
                  placeholder="Votre prénom"
                  required
                  disabled={loading}
                />
                <label htmlFor="prenom">Prénom *</label>
              </div>
            </div>

            <div className="form-group">
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="votre@email.com"
                required
                disabled={loading}
              />
              <label htmlFor="email">Email *</label>
            </div>

            <div className="form-group">
              <input
                type="tel"
                id="numTel"
                name="numTel"
                value={formData.numTel}
                onChange={handleChange}
                placeholder="+261 34 00 000 00"
                disabled={loading}
              />
              <label htmlFor="numTel">Numéro de téléphone</label>
            </div>

            <div className="form-row">
              <div className="form-group">
                <input
                  type="password"
                  id="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="••••••••"
                  required
                  disabled={loading}
                />
                <label htmlFor="password">Mot de passe *</label>
              </div>

              <div className="form-group">
                <input
                  type="password"
                  id="confirmPassword"
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  placeholder="••••••••"
                  required
                  disabled={loading}
                />
                <label htmlFor="confirmPassword">Confirmer *</label>
              </div>
            </div>

            <button type="submit" className="register-button" disabled={loading}>
              {loading ? 'Inscription...' : 'S\'inscrire'}
            </button>
          </form>

          <div className="login-link">
            Déjà un compte ? <a href="/login">Se connecter</a>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Register;