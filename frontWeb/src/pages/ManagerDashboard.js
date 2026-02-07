import React, { useEffect, useState } from 'react';
import { FaClipboardList, FaPlus, FaHourglassHalf, FaCheckCircle, FaRulerCombined } from 'react-icons/fa';
import { BiMoney } from 'react-icons/bi';
import './ManagerDashboard.css';

function ManagerDashboard() {
  const [stats, setStats] = useState({
    totalSignalements: 0,
    nouveau: 0,
    enCours: 0,
    termine: 0,
    surfaceTotal: 0,
    chiffreAffaire: 0,
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
      const processedStats = {
        totalSignalements: parseInt(statsData.totalSignalements) || 0,
        nouveau: parseInt(statsData.nouveau) || 0,
        enCours: parseInt(statsData.enCours) || 0,
        termine: parseInt(statsData.termine) || 0,
        surfaceTotal: parseFloat(statsData.surfaceTotal) || 0,
        chiffreAffaire: parseFloat(statsData.chiffreAffaire) || 0,
        avancement: parseInt(statsData.avancement) || 0
      };
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
          <div className="stat-icon"><FaClipboardList /></div>
          <div className="stat-content">
            <h3>Total Signalements</h3>
            <div className="stat-value">{stats.totalSignalements}</div>
          </div>
        </div>
        <div className="stat-card red">
          <div className="stat-icon"><FaPlus /></div>
          <div className="stat-content">
            <h3>Nouveaux</h3>
            <div className="stat-value">{stats.nouveau}</div>
          </div>
        </div>
        <div className="stat-card orange">
          <div className="stat-icon"><FaHourglassHalf /></div>
          <div className="stat-content">
            <h3>En cours</h3>
            <div className="stat-value">{stats.enCours}</div>
          </div>
        </div>
        <div className="stat-card green">
          <div className="stat-icon"><FaCheckCircle /></div>
          <div className="stat-content">
            <h3>Terminés</h3>
            <div className="stat-value">{stats.termine}</div>
          </div>
        </div>
        <div className="stat-card blue">
          <div className="stat-icon"><FaRulerCombined /></div>
          <div className="stat-content">
            <h3>Surface Totale</h3>
            <div className="stat-value">{stats.surfaceTotal} m²</div>
          </div>
        </div>
        <div className="stat-card purple">
          <div className="stat-icon"><BiMoney /></div>
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
                  <td>
                    {signal.lastHistoriqueDate
                      ? formatDate(signal.lastHistoriqueDate)
                      : signal.createdAt
                      ? formatDate(signal.createdAt)
                      : 'N/A'}
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
    </div>
  );
}

export default ManagerDashboard;
