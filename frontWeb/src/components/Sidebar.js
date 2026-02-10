import React, { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { FaMap, FaChartBar, FaClipboardList, FaUsers, FaChartLine, FaSync, FaCog, FaDollarSign } from 'react-icons/fa';
import './Sidebar.css';

function Sidebar({ user, onLogout }) {
  const [isOnline, setIsOnline] = useState(false);
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
    { path: '/', label: 'Carte', icon: <FaMap /> },
    { path: '/dashboard', label: 'Tableau de bord', icon: <FaChartBar /> },
    { path: '/signalements', label: 'Signalements', icon: <FaClipboardList /> },
    { path: '/users', label: 'Utilisateurs', icon: <FaUsers /> },
    { path: '/statistics', label: 'Statistiques', icon: <FaChartLine /> },
    { path: '/prix-forfaitaire', label: 'Prix Forfaitaire', icon: <FaDollarSign /> },
    { path: '/settings', label: 'Paramètres', icon: <FaCog /> }
  ];

  return (
    <aside className="sidebar">
      {/* Header avec Firebase status */}
      <div className="sidebar-header">
        <div className="sidebar-brand">
          <span className="brand-icon"><FaMap /></span>
          <span className="brand-text">Manager</span>
        </div>
        <div className="firebase-status">
          <span
            className={`status-dot ${isOnline ? 'online' : 'offline'}`}
            title={isOnline ? 'Connecté à Firebase' : 'Hors ligne Firebase'}
          ></span>
          <span className="status-text">
            {isOnline ? 'En ligne' : 'Hors ligne'}
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
