import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './Navbar.css';

function Navbar({ user, onLogout }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    if (onLogout) onLogout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">


        <div className={`navbar-menu ${menuOpen ? 'active' : ''}`}>
          {user ? (
            <>
              <Link to="/dashboard" className="nav-link">Tableau de bord</Link>
              <Link to="/signalements" className="nav-link">Signalements</Link>
              <Link to="/users" className="nav-link">Utilisateurs</Link>
              <Link to="/sync" className="nav-link">Synchronisation</Link>
              
              <div className="nav-user">
                <span className="user-name">{user.prenom} {user.nom}</span>
                <button onClick={handleLogout} className="logout-btn">
                  Déconnexion
                </button>
              </div>
            </>
          ) : (
            <>
           
            </>
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
