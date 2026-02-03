import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './Navbar.css';

function Navbar({ user, onLogout }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [isOnline, setIsOnline] = useState(null);
  const navigate = useNavigate();

  // Vérifie la connectivité Firebase périodiquement
  useEffect(() => {
    let intervalId;
    const checkConnectivity = async () => {
      try {
        const res = await fetch('http://localhost:8080/api/connectivity/firebase');
        const online = await res.json();
        setIsOnline(online === true);
      } catch {
        setIsOnline(false);
      }
    };
    checkConnectivity();
    intervalId = setInterval(checkConnectivity, 10000); // toutes les 10s
    return () => clearInterval(intervalId);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    if (onLogout) onLogout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        {/* Indicateur de connectivité Firebase */}
        <div className="firebase-status-indicator">
          <span
            className={`status-bubble ${isOnline === true ? 'online' : isOnline === false ? 'offline' : ''}`}
            title={isOnline === true ? 'Connecté à Firebase' : isOnline === false ? 'Hors ligne Firebase' : 'Vérification...'}
          ></span>
          <span className="status-label">
            {isOnline === true ? 'En ligne' : isOnline === false ? 'Hors ligne' : '...'}
          </span>
        </div>

        <div className={`navbar-menu ${menuOpen ? 'active' : ''}`}>
          {user ? (
            <>
              <Link to="/dashboard" className="nav-link">Tableau de bord</Link>
              <Link to="/signalements" className="nav-link">Signalements</Link>
              <Link to="/users" className="nav-link">Utilisateurs</Link>
              <Link to="/statistics" className="nav-link">Statistiques</Link>
              <Link to="/sync" className="nav-link">Synchronisation</Link>
              <Link to="/settings" className="nav-link">Paramètres</Link>
              <div className="nav-user">
                <span className="user-name">{user.prenom} {user.nom}</span>
                <button onClick={handleLogout} className="logout-btn">
                  Déconnexion
                </button>
              </div>
            </>
          ) : (
            <></>
          )}
        </div>

        <button 
          className="navbar-toggle"
          onClick={() => setMenuOpen(!menuOpen)}
        >
          <span></span>
          <span></span>
          <span></span>
        </button>
      </div>
    </nav>
  );
}

export default Navbar;
