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
import PrixForfaitaire from './pages/PrixForfaitaire';
import { startSessionMonitoring, stopSessionMonitoring, checkTokenValidity } from './services/authService';
import './App.css';

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Vérifier si l'utilisateur est déjà connecté au démarrage
    const checkExistingSession = async () => {
      const storedUser = localStorage.getItem('user');
      const token = localStorage.getItem('token');
      
      if (storedUser && token) {
        try {
          // Vérifier si le token est toujours valide
          const isValid = await checkTokenValidity();
          
          if (isValid) {
            // Token valide, restaurer la session
            const userData = JSON.parse(storedUser);
            setUser(userData);
            
            // Démarrer le monitoring de session
            startSessionMonitoring();
          } else {
            // Token invalide, nettoyer le localStorage
            localStorage.removeItem('user');
            localStorage.removeItem('token');
          }
        } catch (error) {
          console.error('Erreur lors de la vérification de la session:', error);
          localStorage.removeItem('user');
          localStorage.removeItem('token');
        }
      }
      
      setLoading(false);
    };
    
    checkExistingSession();
  }, []);

  const handleLogin = (userData) => {
    // Stocker les rôles avec l'utilisateur
    const userWithRoles = { ...userData.user, roles: userData.roles };
    setUser(userWithRoles);
    localStorage.setItem('user', JSON.stringify(userWithRoles));
    
    // Démarrer le monitoring de session après la connexion
    startSessionMonitoring();
  };

  const handleLogout = () => {
    setUser(null);
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    stopSessionMonitoring();
  };

  // Afficher un écran de chargement pendant la vérification de la session
  if (loading) {
    return (
      <div className="App" style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh',
        backgroundColor: '#f5f5f5'
      }}>
        <div style={{ textAlign: 'center' }}>
          <div className="spinner" style={{
            border: '4px solid #f3f3f3',
            borderTop: '4px solid #3498db',
            borderRadius: '50%',
            width: '40px',
            height: '40px',
            animation: 'spin 1s linear infinite',
            margin: '0 auto 20px'
          }}></div>
          <p>Chargement...</p>
        </div>
      </div>
    );
  }

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
            {/* Route publique - Carte accessible pour tous */}
            <Route 
              path="/" 
              element={<VisitorMap />} 
            />
            
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
            <Route 
              path="/prix-forfaitaire" 
              element={
                <ProtectedRoute>
                  <PrixForfaitaire />
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