import React, { useEffect, useState } from 'react';
import './ManagerDashboard.css';

function ManagerDashboard() {
  const [stats, setStats] = useState({
    totalSignalements: 0,
    nouveau: 0,
    enCours: 0,
    termine: 0,
    surfaceTotal: 0,
    budgetTotal: 0,
    avancement: 0
  });
  const [recentSignalements, setRecentSignalements] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      const token = localStorage.getItem('token');
      
      const [signalementsRes, statsRes] = await Promise.all([
        fetch('http://localhost:8080/api/signalements/recent', {
          headers: { 'Authorization': `Bearer ${token}` }
        }),
        fetch('http://localhost:8080/api/signalements/stats', {
          headers: { 'Authorization': `Bearer ${token}` }
        })
      ]);

      const signalementsData = await signalementsRes.json();
      const statsData = await statsRes.json();

      setRecentSignalements(signalementsData);
      setStats(statsData);
    } catch (error) {
      console.error('Erreur:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'MGA',
      minimumFractionDigits: 0
    }).format(amount);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('fr-FR');
  };

  const getStatusLabel = (status) => {
    const labels = {
      'nouveau': 'Nouveau',
      'en_cours': 'En cours',
      'termine': 'Terminé'
    };
    return labels[status] || status;
  };

  const getStatusClass = (status) => {
    return `status-badge status-${status}`;
  };

  if (loading) {
    return <div className="loading">Chargement...</div>;
  }

  return (
    <div className="manager-dashboard">
      <header className="dashboard-header">
        <h1>Tableau de bord Manager</h1>
        <p>Vue d'ensemble des travaux routiers</p>
      </header>

      <div className="stats-grid">
        <div className="stat-card primary">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <h3>Total Signalements</h3>
            <div className="stat-value">{stats.totalSignalements}</div>
          </div>
        </div>

        <div className="stat-card red">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <h3>Nouveaux</h3>
            <div className="stat-value">{stats.nouveau}</div>
          </div>
        </div>

        <div className="stat-card orange">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <h3>En cours</h3>
            <div className="stat-value">{stats.enCours}</div>
          </div>
        </div>

        <div className="stat-card green">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <h3>Terminés</h3>
            <div className="stat-value">{stats.termine}</div>
          </div>
        </div>

        <div className="stat-card blue">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <h3>Surface Totale</h3>
            <div className="stat-value">{stats.surfaceTotal} m²</div>
          </div>
        </div>

        <div className="stat-card purple">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <h3>Budget Total</h3>
            <div className="stat-value">{formatCurrency(stats.budgetTotal)}</div>
          </div>
        </div>
      </div>

      <div className="progress-section">
        <h2>Avancement global</h2>
        <div className="progress-bar-container">
          <div className="progress-bar" style={{ width: `${stats.avancement}%` }}>
            <span className="progress-text">{stats.avancement}%</span>
          </div>
        </div>
      </div>

      <div className="recent-section">
        <h2>Signalements récents</h2>
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Date</th>
                <th>Localisation</th>
                <th>Statut</th>
                <th>Surface</th>
                <th>Budget</th>
                <th>Entreprise</th>
              </tr>
            </thead>
            <tbody>
              {recentSignalements.map((signal) => (
                <tr key={signal.id}>
                  <td>#{signal.id}</td>
                  <td>{formatDate(signal.date)}</td>
                  <td>
                    <div className="location">
                       {signal.latitude.toFixed(4)}, {signal.longitude.toFixed(4)}
                    </div>
                  </td>
                  <td>
                    <span className={getStatusClass(signal.status)}>
                      {getStatusLabel(signal.status)}
                    </span>
                  </td>
                  <td>{signal.surface} m²</td>
                  <td>{formatCurrency(signal.budget)}</td>
                  <td>{signal.entreprise || 'Non attribuée'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="quick-actions">
        <h2>Actions rapides</h2>
        <div className="action-buttons">
          <button className="action-btn" onClick={() => window.location.href = '/signalements'}>
             Gérer les signalements
          </button>
          <button className="action-btn" onClick={() => window.location.href = '/users'}>
             Gérer les utilisateurs
          </button>
          <button className="action-btn" onClick={() => window.location.href = '/sync'}>
            Synchroniser
          </button>
        </div>
      </div>
    </div>
  );
}

export default ManagerDashboard;
