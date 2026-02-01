import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Navbar from './components/Navbar';
import Login from './components/Login';
import Register from './components/Register';
import VisitorMap from './pages/VisitorMap';
import ManagerDashboard from './pages/ManagerDashboard';
import SignalementManagement from './pages/SignalementManagement';
import UserManagement from './pages/UserManagement';
import Synchronization from './pages/Synchronization';
import Settings from './pages/Settings';
import './App.css';

function App() {
  const [user, setUser] = useState(null);

  useEffect(() => {
    // Charger l'utilisateur depuis le localStorage
    const storedUser = localStorage.getItem('user');
    if (storedUser && storedUser !== 'undefined') {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        console.error('Error parsing stored user:', e);
        localStorage.removeItem('user');
      }
    }
  }, []);

  const handleLogin = (userData) => {
    setUser(userData);
    localStorage.setItem('user', JSON.stringify(userData));
  };

  const handleLogout = () => {
    setUser(null);
    localStorage.removeItem('user');
    localStorage.removeItem('token');
  };

  // Composant pour protéger les routes Manager
  const ProtectedRoute = ({ children }) => {
    const token = localStorage.getItem('token');
    return token ? children : <Navigate to="/login" />;
  };

  return (
    <Router>
      <div className="App">
        {/* ⭐ NAVBAR EN PREMIER - Toujours affichée si user connecté */}
        {user && <Navbar user={user} onLogout={handleLogout} />}
        
        {/* ⭐ CONTENU PRINCIPAL après la navbar */}
        <main className="main-content">
          <Routes>
            {/* Route publique - Carte visiteur */}
            <Route path="/" element={<VisitorMap />} />
            
            {/* Routes d'authentification - Avec wrapper pour centrage */}
            <Route 
              path="/login" 
              element={
                user ? <Navigate to="/dashboard" /> : (
                  <div className="auth-page">
                    <Login onLogin={handleLogin} />
                  </div>
                )
              } 
            />
            <Route 
              path="/register" 
              element={
                user ? <Navigate to="/dashboard" /> : (
                  <div className="auth-page">
                    <Register />
                  </div>
                )
              } 
            />
            
            {/* Routes protégées Manager */}
            <Route 
              path="/dashboard" 
              element={
                <ProtectedRoute>
                  <ManagerDashboard />
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/signalements" 
              element={
                <ProtectedRoute>
                  <SignalementManagement />
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/users" 
              element={
                <ProtectedRoute>
                  <UserManagement />
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/sync" 
              element={
                <ProtectedRoute>
                  <Synchronization />
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/settings" 
              element={
                <ProtectedRoute>
                  <Settings />
                </ProtectedRoute>
              } 
            />
            
            {/* Redirection pour les routes non trouvées */}
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;