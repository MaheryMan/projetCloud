import React, { useState, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { FaMap, FaChartBar, FaClipboardList, FaUsers, FaChartLine, FaCog, FaDoorOpen } from 'react-icons/fa';
import './Sidebar.css';

function Sidebar({ user, onLogout }) {
  const [isOnline, setIsOnline] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const isFirstCheck = useRef(true);
  const location = useLocation();
  const navigate = useNavigate();

  // Vérifie la connectivité Firebase périodiquement
  useEffect(() => {
    let intervalId;
    const checkConnectivity = async () => {
      try {
        if (isFirstCheck.current) {
          setIsLoading(true);
        }
        const res = await fetch('http://localhost:8080/api/connectivity/firebase');
        const online = await res.json();
        setIsOnline(online === true);
      } catch {
        setIsOnline(false);
      } finally {
        if (isFirstCheck.current) {
          setIsLoading(false);
          isFirstCheck.current = false;
        }
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
    { path: '/settings', label: 'Paramètres', icon: <FaCog /> }
  ];

  const getStatusClass = () => {
    if (isLoading) return 'loading';
    return isOnline ? 'online' : 'offline';
  };

  const getStatusText = () => {
    if (isLoading) return 'Connexion...';
    return isOnline ? 'En ligne' : 'Hors ligne';
  };

  return (
    <aside className="sidebar">
        <div className="sidebar-header">
          <div className="sidebar-brand">
            <span className="brand-icon" style={{ color: 'white', fontSize: '32px' }}>
          <FaMap />
            </span>
            <span className="brand-text" style={{ color: 'white', fontSize: '24px', fontWeight: 'bold' }}>Manager</span>
          </div>
          <div className="firebase-status">
            <span
          className={`status-dot ${getStatusClass()}`}
          title={
            isLoading
              ? 'Vérification de la connexion...'
              : isOnline
              ? 'Connecté à Firebase'
              : 'Hors ligne Firebase'
          }
            />
            <span className="status-text">{getStatusText()}</span>
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
            <div className="user-header">
              <div className="user-avatar">
                {user.prenom?.charAt(0)}
                {user.nom?.charAt(0)}
              </div>
              <button
                onClick={handleLogout}
                className="logout-btn"
                title="Déconnexion"
                aria-label="Déconnexion"
              >
                <FaDoorOpen />
              </button>
            </div>
            <div className="user-details">
              <div className="user-name">{user.prenom} {user.nom}</div>
              <div className="user-role">Manager</div>
            </div>
          </div>
        </div>
      )}
    </aside>
  );
}

export default Sidebar;