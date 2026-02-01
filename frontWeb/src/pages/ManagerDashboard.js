import React, { useEffect, useState } from 'react';
import './ManagerDashboard.css';

function ManagerDashboard() {
  const [stats, setStats] = useState({
    totalSignalements: 0,
    nouveau: 0,
    enCours: 0,
    termine: 0,
    surfaceTotal: 0,
    chiffreAffaire: 0, // Somme totale de tous les budgets
    avancement: 0 // (terminés / (en cours + terminés)) * 100
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

      // Debug - afficher les statuts des signalements
      console.log('Tous les signalements:', signalementsData);
      signalementsData.forEach(s => {
        console.log(`Signalement #${s.id}: statut=${s.idStatus}`);
      });

      // Debug
      console.log('Stats reçues:', statsData);

      // Convertir les valeurs en nombres
      const processedStats = {
        totalSignalements: parseInt(statsData.totalSignalements) || 0,
        nouveau: parseInt(statsData.nouveau) || 0,
        enCours: parseInt(statsData.enCours) || 0,
        termine: parseInt(statsData.termine) || 0,
        surfaceTotal: parseFloat(statsData.surfaceTotal) || 0,
        chiffreAffaire: parseFloat(statsData.chiffreAffaire) || 0,
        avancement: parseInt(statsData.avancement) || 0
      };

      console.log('Stats traitées:', processedStats);
      console.log('Formule avancement: ((', processedStats.enCours, ' × 0.5) + ', processedStats.termine, ') / ', 
        (processedStats.nouveau + processedStats.enCours + processedStats.termine), ' × 100 = ', processedStats.avancement, '%');

      setRecentSignalements(signalementsData);
      setStats(processedStats);
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

  const getStatusLabel = (statusOrId) => {
    const map = {
      1: 'Actif',
      2: 'Bloqué',
      3: 'Inactif',
      4: 'Nouveau',
      5: 'En cours',
      6: 'Terminé',
      7: 'Annulé',
      'nouveau': 'Nouveau',
      'en_cours': 'En cours',
      'termine': 'Terminé',
      'annule': 'Annulé'
    };
    return map[statusOrId] || statusOrId;
  };

  const getStatusClass = (status) => {
    return `status-badge status-${status}`;
  };

  const getEntrepriseName = (idEntreprise) => {
    return idEntreprise ? idEntreprise : 'Non attribuée';
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
            <h3>Chiffre d'affaire</h3>
            <div className="stat-value">{formatCurrency(stats.chiffreAffaire)}</div>
          </div>
        </div>
      </div>

      <div className="progress-section">
        <h2>Avancement global</h2>
        <div className="progress-info">
          <span className="progress-label">
            {stats.nouveau + stats.enCours + stats.termine > 0 
              ? `${stats.enCours} en cours · ${stats.termine} terminés · ${stats.nouveau} nouveaux`
              : 'Aucun signalement'}
          </span>
        </div>
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
                      <td>
                    {signal.typeSignalement?.createdAt
                      ? formatDate(signal.typeSignalement.createdAt)
                      : ''}
                  </td>
                  <td>
                    <div className="location">
                       {signal.latitude?.toFixed(4)}, {signal.longitude?.toFixed(4)}
                    </div>
                  </td>
                  <td>
                    <span className={getStatusClass(signal.idStatus)}>
                      {getStatusLabel(signal.idStatus)}
                    </span>
                  </td>
                  <td>{signal.surfaceM2} m²</td>
                  <td>{formatCurrency(signal.budget)}</td>
                  <td>{getEntrepriseName(signal.idEntreprise)}</td>
                    
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
