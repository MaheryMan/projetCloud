import React, { useEffect, useState } from 'react';
import { fetchWithAuth } from '../services/authService';
import './Statistics.css';

function Statistics() {
  const [statistics, setStatistics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [historiques, setHistoriques] = useState([]);
  const [signalements, setSignalements] = useState([]);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');

      // Récupérer les signalements et leurs historiques
      const [signalRes, histoRes] = await Promise.all([
        fetchWithAuth('http://localhost:8080/api/signalements'),
        fetchWithAuth('http://localhost:8080/api/historiques')
      ]);

      if (!signalRes.ok) {
        throw new Error('Erreur lors du chargement des signalements');
      }

      const signalData = await signalRes.json();
      setSignalements(signalData);

      // Si l'endpoint historiques n'existe pas, on calcule depuis les signalements
      let histoData = [];
      if (histoRes.ok) {
        histoData = await histoRes.json();
        setHistoriques(histoData);
      }

      // Calculer les statistiques
      calculateStatistics(signalData, histoData);
      setError('');
    } catch (err) {
      console.error('Erreur:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const calculateStatistics = (signals, histos) => {
    // Calculer les délais de traitement
    const delais = [];
    const delaisByStatus = {
      nouveau_to_encours: [],
      encours_to_termine: [],
      total: []
    };

    signals.forEach(signal => {
      // Récupérer tous les historiques de ce signalement
      const signalHistos = histos.filter(h => h.idSignalement === signal.id);
      
      if (signalHistos.length > 0) {
        // Trier par date croissante pour avoir le premier et le dernier
        signalHistos.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
        
        const premierHisto = signalHistos[0];
        const dernierHisto = signalHistos[signalHistos.length - 1];
        
        const debut = new Date(premierHisto.createdAt);
        const fin = new Date(dernierHisto.createdAt);
        const delaiTotal = (fin - debut) / (1000 * 60 * 60 * 24); // En jours

        delais.push({
          id: signal.id,
          delai: delaiTotal,
          status: signal.idStatus,
          type: signal.typeSignalement?.libelle,
          dateDebut: premierHisto.createdAt,
          dateFin: dernierHisto.createdAt
        });

        if (signal.idStatus === 6) { // Terminé
          delaisByStatus.total.push(delaiTotal);
        }
      }
    });

    // Statistiques par statut
    const parStatut = {
      nouveau: signals.filter(s => s.idStatus === 4).length,
      enCours: signals.filter(s => s.idStatus === 5).length,
      termine: signals.filter(s => s.idStatus === 6).length
    };

    // Délai moyen
    const delaiMoyen = delais.length > 0
      ? delais.reduce((sum, d) => sum + d.delai, 0) / delais.length
      : 0;

    const delaiMoyenTermine = delaisByStatus.total.length > 0
      ? delaisByStatus.total.reduce((sum, d) => sum + d, 0) / delaisByStatus.total.length
      : 0;

    // Délai min/max
    const delaisNonZero = delais.map(d => d.delai).filter(d => d > 0);
    const delaiMin = delaisNonZero.length > 0 ? Math.min(...delaisNonZero) : 0;
    const delaiMax = delaisNonZero.length > 0 ? Math.max(...delaisNonZero) : 0;

    // Statistiques par type
    const parType = {};
    signals.forEach(signal => {
      const type = signal.typeSignalement?.libelle || 'Inconnu';
      if (!parType[type]) {
        parType[type] = { count: 0, delais: [] };
      }
      parType[type].count++;

      const delaiSignal = delais.find(d => d.id === signal.id);
      if (delaiSignal) {
        parType[type].delais.push(delaiSignal.delai);
      }
    });

    // Calculer délai moyen par type
    Object.keys(parType).forEach(type => {
      const typeDelais = parType[type].delais;
      parType[type].delaiMoyen = typeDelais.length > 0
        ? typeDelais.reduce((sum, d) => sum + d, 0) / typeDelais.length
        : 0;
    });

    setStatistics({
      total: signals.length,
      parStatut,
      delaiMoyen,
      delaiMoyenTermine,
      delaiMin,
      delaiMax,
      delais,
      parType
    });
  };

  const formatDelai = (jours) => {
    if (jours === 0) return '0 jour';
    if (jours < 1) return `${(jours * 24).toFixed(1)} heures`;
    return `${jours.toFixed(1)} jours`;
  };

  const getStatusClass = (status) => {
    if (status === 4) return 'status-nouveau';
    if (status === 5) return 'status-en-cours';
    if (status === 6) return 'status-termine';
    return '';
  };

  if (loading) {
    return (
      <div className="statistics-container">
        <h1>Statistiques de Traitement</h1>
        <div className="loading">Chargement des statistiques...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="statistics-container">
        <h1>Statistiques de Traitement</h1>
        <div className="error">{error}</div>
      </div>
    );
  }

  return (
    <div className="statistics-container">
      <h1> Statistiques de Traitement des Travaux</h1>

      {/* Vue d'ensemble */}
      <div className="stats-cards">
        <div className="stat-card">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <h3>Total Signalements</h3>
            <div className="stat-value">{statistics.total}</div>
          </div>
        </div>

        <div className="stat-card highlight">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <h3>Délai Moyen Global</h3>
            <div className="stat-value">{formatDelai(statistics.delaiMoyen)}</div>
            <div className="stat-subtitle">Tous statuts confondus</div>
          </div>
        </div>

        <div className="stat-card highlight">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <h3>Délai Moyen (Terminés)</h3>
            <div className="stat-value">{formatDelai(statistics.delaiMoyenTermine)}</div>
            <div className="stat-subtitle">{statistics.parStatut.termine} travaux terminés</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <h3>Délai Min / Max</h3>
            <div className="stat-value-small">
              Min: {formatDelai(statistics.delaiMin)}
            </div>
            <div className="stat-value-small">
              Max: {formatDelai(statistics.delaiMax)}
            </div>
          </div>
        </div>
      </div>

      {/* Répartition par statut */}
      <div className="stats-section">
        <h2>Répartition par Statut</h2>
        <div className="stats-table-wrapper">
          <table className="stats-table">
            <thead>
              <tr>
                <th>Statut</th>
                <th>Nombre</th>
                <th>Pourcentage</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><span className="status-badge status-nouveau">Nouveau</span></td>
                <td>{statistics.parStatut.nouveau}</td>
                <td>{((statistics.parStatut.nouveau / statistics.total) * 100).toFixed(1)}%</td>
              </tr>
              <tr>
                <td><span className="status-badge status-en-cours">En cours</span></td>
                <td>{statistics.parStatut.enCours}</td>
                <td>{((statistics.parStatut.enCours / statistics.total) * 100).toFixed(1)}%</td>
              </tr>
              <tr>
                <td><span className="status-badge status-termine">Terminé</span></td>
                <td>{statistics.parStatut.termine}</td>
                <td>{((statistics.parStatut.termine / statistics.total) * 100).toFixed(1)}%</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Statistiques par type */}
      <div className="stats-section">
        <h2>Délai Moyen par Type de Travaux</h2>
        <div className="stats-table-wrapper">
          <table className="stats-table">
            <thead>
              <tr>
                <th>Type de Travaux</th>
                <th>Nombre</th>
                <th>Délai Moyen</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(statistics.parType)
                .sort((a, b) => b[1].count - a[1].count)
                .map(([type, data]) => (
                  <tr key={type}>
                    <td>{type}</td>
                    <td>{data.count}</td>
                    <td>{formatDelai(data.delaiMoyen)}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Tableau détaillé des délais */}
      <div className="stats-section">
        <h2>Détails des Délais de Traitement</h2>
        <div className="stats-table-wrapper">
          <table className="stats-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Type</th>
                <th>Délai</th>
                <th>Statut</th>
              </tr>
            </thead>
            <tbody>
              {statistics.delais
                .sort((a, b) => b.delai - a.delai)
                .slice(0, 20)
                .map((item) => (
                  <tr key={item.id}>
                    <td>#{item.id}</td>
                    <td>{item.type || 'N/A'}</td>
                    <td><strong>{formatDelai(item.delai)}</strong></td>
                    <td>
                      <span className={`status-badge ${getStatusClass(item.status)}`}>
                        {item.status === 4 ? 'Nouveau' : item.status === 5 ? 'En cours' : 'Terminé'}
                      </span>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
        {statistics.delais.length > 20 && (
          <p className="stats-note">Affichage des 20 signalements avec les plus longs délais</p>
        )}
      </div>
    </div>
  );
}

export default Statistics;
