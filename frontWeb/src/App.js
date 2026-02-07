import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import FirebaseStatusBubble from './components/FirebaseStatusBubble';
import Login from './components/Login';
import Register from './components/Register';
import Sidebar from './components/Sidebar';
import VisitorMap from './pages/VisitorMap';
import ManagerDashboard from './pages/ManagerDashboard';
import SignalementManagement from './pages/SignalementManagement';
import SignalementDetails from './pages/SignalementDetails';
import UserManagement from './pages/UserManagement';
import Statistics from './pages/Statistics';
import Settings from './pages/Settings';
import { startSessionMonitoring, stopSessionMonitoring } from './services/authService';
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

    // Démarrer le monitoring de session si utilisateur connecté
    let monitoringId = null;
    const token = localStorage.getItem('token');
    if (token) {
      // Vérifier toutes les 5 minutes si la session est toujours valide
      monitoringId = startSessionMonitoring(5);
    }

    // Nettoyer le monitoring au démontage du composant
    return () => {
      if (monitoringId) {
        stopSessionMonitoring(monitoringId);
      }
    };
  }, []);

  const handleLogin = (userData) => {
    // Stocker les rôles avec l'utilisateur
    const userWithRoles = { ...userData.user, roles: userData.roles };
    setUser(userWithRoles);
    localStorage.setItem('user', JSON.stringify(userWithRoles));
  };

  const handleLogout = () => {
    setUser(null);
    localStorage.removeItem('user');
    localStorage.removeItem('token');
  };

  // Composant pour protéger les routes Manager
  const ProtectedRoute = ({ children }) => {
    const token = localStorage.getItem('token');
    if (!token) return <Navigate to="/login" />;
    
    // Vérifier si l'utilisateur est un manager
    if (!user || !user.roles || !user.roles.includes('Manager')) {
      return <Navigate to="/" />;
    }
    
    return children;
  };

  return (
    <Router>
      <div className="App">
        {/* Firebase status bubble seulement pour les utilisateurs connectés non-managers */}
        {user && !(user.roles && user.roles.includes('Manager')) && <FirebaseStatusBubble />}
        
        {/* ⭐ SIDEBAR - Affichée seulement pour les managers */}
        {user && user.roles && user.roles.includes('Manager') && (
          <Sidebar user={user} onLogout={handleLogout} />
        )}
        
        {/* ⭐ CONTENU PRINCIPAL avec padding pour la sidebar */}
        <main className={`main-content ${!user || !(user.roles && user.roles.includes('Manager')) ? 'visitor-mode' : 'with-sidebar'}`}>
          <Routes>
            {/* Route publique - Carte visiteur */}
            <Route path="/" element={<VisitorMap />} />
            
            {/* Routes d'authentification - Avec wrapper pour centrage */}
            <Route 
              path="/login" 
              element={
                user ? (
                  <Navigate to={user.roles && user.roles.includes('Manager') ? "/dashboard" : "/"} />
                ) : (
                  <div className="auth-page">
                    <Login onLogin={handleLogin} />
                  </div>
                )
              } 
            />
            <Route 
              path="/register" 
              element={
                user ? (
                  user.roles && user.roles.includes('Manager') 
                    ? <Navigate to="/dashboard" /> 
                    : <Navigate to="/" />
                ) : (
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
              path="/signalements/:id" 
              element={
                <ProtectedRoute>
                  <SignalementDetails />
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
              path="/statistics" 
              element={
                <ProtectedRoute>
                  <Statistics />
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