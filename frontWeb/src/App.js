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
import './App.css';

function App() {
  const [user, setUser] = useState(null);

  useEffect(() => {
    // Charger l'utilisateur depuis le localStorage
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
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
        <Navbar user={user} onLogout={handleLogout} />
        
        <Routes>
          {/* Route publique - Carte visiteur */}
          <Route path="/" element={<VisitorMap />} />
          
          {/* Routes d'authentification */}
          <Route 
            path="/login" 
            element={user ? <Navigate to="/dashboard" /> : <Login onLogin={handleLogin} />} 
          />
          <Route 
            path="/register" 
            element={user ? <Navigate to="/dashboard" /> : <Register />} 
          />
          
          {/* Routes protégées Manager */}
          <Route 
            path="/dashboard" 
            element={
                <ManagerDashboard />
            } 
          />
          <Route 
            path="/signalements" 
            element={
              
                <SignalementManagement />
              
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
          
          {/* Redirection pour les routes non trouvées */}
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
