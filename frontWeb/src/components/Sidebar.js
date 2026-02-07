import React, { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import './Sidebar.css';

function Sidebar({ user, onLogout }) {
  const [isOnline, setIsOnline] = useState(null);
  const location = useLocation();
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
    navigate('/');
  };

  const menuItems = [
    { path: '/', label: 'Carte', icon: '🗺️' },
    { path: '/dashboard', label: 'Tableau de bord', icon: '📊' },
    { path: '/signalements', label: 'Signalements', icon: '📋' },
    { path: '/users', label: 'Utilisateurs', icon: '👥' },
    { path: '/statistics', label: 'Statistiques', icon: '📈' },
    { path: '/sync', label: 'Synchronisation', icon: '🔄' },
    { path: '/settings', label: 'Paramètres', icon: '⚙️' }
  ];

  return (
    <aside className="sidebar">
      {/* Header avec Firebase status */}
      <div className="sidebar-header">
        <div className="sidebar-brand">
          <span className="brand-icon">🗺️</span>
          <span className="brand-text">Manager</span>
        </div>
        <div className="firebase-status">
          <span
            className={`status-dot ${isOnline === true ? 'online' : isOnline === false ? 'offline' : 'loading'}`}
            title={isOnline === true ? 'Connecté à Firebase' : isOnline === false ? 'Hors ligne Firebase' : 'Vérification...'}
          ></span>
          <span className="status-text">
            {isOnline === true ? 'En ligne' : isOnline === false ? 'Hors ligne' : 'Vérification...'}
          </span>
        </div>
      </div>

      {/* Menu de navigation */}
      <nav className="sidebar-nav">
        {menuItems.map((item) => (
          <Link
            key={item.path}
            to={item.path}
            className={`nav-item ${location.pathname === item.path ? 'active' : ''}`}
          >
            <span className="nav-icon">{item.icon}</span>
            <span className="nav-label">{item.label}</span>
          </Link>
        ))}
      </nav>

      {/* User info en bas */}
      {user && (
        <div className="sidebar-footer">
          <div className="user-info">
            <div className="user-avatar">
              {user.prenom?.charAt(0)}{user.nom?.charAt(0)}
            </div>
            <div className="user-details">
              <div className="user-name">{user.prenom} {user.nom}</div>
              <div className="user-role">Manager</div>
            </div>
            <button onClick={handleLogout} className="logout-icon-btn" title="Déconnexion">
              🚪
            </button>
          </div>
        </div>
      )}
    </aside>
  );
}

export default Sidebar;
