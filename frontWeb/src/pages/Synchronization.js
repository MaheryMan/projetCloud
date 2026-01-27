import React, { useState, useEffect } from 'react';
import './Synchronization.css';

function Synchronization() {
  const [syncStatus, setSyncStatus] = useState({
    lastSync: null,
    isOnline: false,
    pendingSignalements: 0,
    pendingUsers: 0
  });
  const [syncing, setSyncing] = useState(false);
  const [syncLog, setSyncLog] = useState([]);
  const [connectionStatus, setConnectionStatus] = useState('checking');

  useEffect(() => {
    checkConnectionStatus();
    fetchSyncStatus();
  }, []);

  const checkConnectionStatus = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/sync/status');
      const data = await response.json();
      setConnectionStatus(data.online ? 'online' : 'offline');
      setSyncStatus(prev => ({ ...prev, isOnline: data.online }));
    } catch (error) {
      setConnectionStatus('offline');
      setSyncStatus(prev => ({ ...prev, isOnline: false }));
    }
  };

  const fetchSyncStatus = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('http://localhost:8080/api/sync/info', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (response.ok) {
        const data = await response.json();
        setSyncStatus(data);
      }
    } catch (error) {
      console.error('Erreur:', error);
    }
  };

  const handleSyncToFirebase = async () => {
    if (!syncStatus.isOnline) {
      alert('❌ Pas de connexion Internet. Synchronisation impossible.');
      return;
    }

    setSyncing(true);
    addLog('🚀 Démarrage de la synchronisation vers Firebase...', 'info');

    try {
      const token = localStorage.getItem('token');
      
      // Envoyer les signalements vers Firebase
      addLog('📤 Envoi des signalements...', 'info');
      const signalementsRes = await fetch('http://localhost:8080/api/sync/push-signalements', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!signalementsRes.ok) throw new Error('Erreur envoi signalements');
      
      const signalementsData = await signalementsRes.json();
      addLog(`✅ ${signalementsData.count} signalements envoyés`, 'success');

      // Envoyer les utilisateurs vers Firebase
      addLog('📤 Envoi des utilisateurs...', 'info');
      const usersRes = await fetch('http://localhost:8080/api/sync/push-users', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!usersRes.ok) throw new Error('Erreur envoi utilisateurs');
      
      const usersData = await usersRes.json();
      addLog(`✅ ${usersData.count} utilisateurs envoyés`, 'success');

      addLog('🎉 Synchronisation vers Firebase terminée avec succès !', 'success');
      
      await fetchSyncStatus();
    } catch (error) {
      console.error('Erreur:', error);
      addLog(`❌ Erreur: ${error.message}`, 'error');
    } finally {
      setSyncing(false);
    }
  };

  const handleSyncFromFirebase = async () => {
    if (!syncStatus.isOnline) {
      alert('❌ Pas de connexion Internet. Synchronisation impossible.');
      return;
    }

    setSyncing(true);
    addLog('🚀 Démarrage de la synchronisation depuis Firebase...', 'info');

    try {
      const token = localStorage.getItem('token');
      
      // Récupérer les signalements depuis Firebase
      addLog('📥 Récupération des signalements...', 'info');
      const signalementsRes = await fetch('http://localhost:8080/api/sync/pull-signalements', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!signalementsRes.ok) throw new Error('Erreur récupération signalements');
      
      const signalementsData = await signalementsRes.json();
      addLog(`✅ ${signalementsData.count} nouveaux signalements récupérés`, 'success');

      addLog('🎉 Synchronisation depuis Firebase terminée avec succès !', 'success');
      
      await fetchSyncStatus();
    } catch (error) {
      console.error('Erreur:', error);
      addLog(`❌ Erreur: ${error.message}`, 'error');
    } finally {
      setSyncing(false);
    }
  };

  const handleFullSync = async () => {
    addLog('🔄 Synchronisation complète...', 'info');
    await handleSyncFromFirebase();
    await handleSyncToFirebase();
  };

  const addLog = (message, type = 'info') => {
    const timestamp = new Date().toLocaleTimeString('fr-FR');
    setSyncLog(prev => [...prev, { message, type, timestamp }]);
  };

  const clearLog = () => {
    setSyncLog([]);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Jamais';
    return new Date(dateString).toLocaleString('fr-FR');
  };

  return (
    <div className="synchronization-page">
      <header className="page-header">
        <h1>Synchronisation Firebase</h1>
        <p>Synchroniser les données avec le cloud</p>
      </header>

      <div className="status-card">
        <div className="connection-status">
          <div className={`status-indicator ${connectionStatus}`}>
            <span className="status-dot"></span>
            <span className="status-text">
              {connectionStatus === 'online' ? '🌐 En ligne' : 
               connectionStatus === 'offline' ? '📴 Hors ligne' : 
               '⏳ Vérification...'}
            </span>
          </div>
          
          <button 
            className="refresh-btn"
            onClick={checkConnectionStatus}
            disabled={syncing}
          >
            🔄 Vérifier la connexion
          </button>
        </div>

        <div className="sync-info">
          <div className="info-item">
            <span className="info-label">Dernière synchronisation:</span>
            <span className="info-value">{formatDate(syncStatus.lastSync)}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Signalements en attente:</span>
            <span className="info-value badge">{syncStatus.pendingSignalements}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Utilisateurs en attente:</span>
            <span className="info-value badge">{syncStatus.pendingUsers}</span>
          </div>
        </div>
      </div>

      <div className="sync-actions">
        <div className="action-card">
          <div className="action-icon">📥</div>
          <h3>Récupérer depuis Firebase</h3>
          <p>Importer les nouveaux signalements depuis le cloud</p>
          <button 
            className="action-btn primary"
            onClick={handleSyncFromFirebase}
            disabled={syncing || !syncStatus.isOnline}
          >
            {syncing ? '⏳ Synchronisation...' : '📥 Récupérer'}
          </button>
        </div>

        <div className="action-card">
          <div className="action-icon">📤</div>
          <h3>Envoyer vers Firebase</h3>
          <p>Exporter les données locales vers le cloud</p>
          <button 
            className="action-btn success"
            onClick={handleSyncToFirebase}
            disabled={syncing || !syncStatus.isOnline}
          >
            {syncing ? '⏳ Synchronisation...' : '📤 Envoyer'}
          </button>
        </div>

        <div className="action-card">
          <div className="action-icon">🔄</div>
          <h3>Synchronisation complète</h3>
          <p>Récupérer et envoyer toutes les données</p>
          <button 
            className="action-btn purple"
            onClick={handleFullSync}
            disabled={syncing || !syncStatus.isOnline}
          >
            {syncing ? '⏳ Synchronisation...' : '🔄 Synchroniser'}
          </button>
        </div>
      </div>

      <div className="sync-log-section">
        <div className="log-header">
          <h2>📋 Journal de synchronisation</h2>
          <button className="clear-btn" onClick={clearLog}>
            🗑️ Effacer
          </button>
        </div>
        
        <div className="log-container">
          {syncLog.length === 0 ? (
            <div className="log-empty">
              Aucune activité de synchronisation
            </div>
          ) : (
            syncLog.map((log, index) => (
              <div key={index} className={`log-entry log-${log.type}`}>
                <span className="log-time">{log.timestamp}</span>
                <span className="log-message">{log.message}</span>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="info-box">
        <h3>ℹ️ Informations sur la synchronisation</h3>
        <ul>
          <li>La synchronisation nécessite une connexion Internet active</li>
          <li><strong>Récupérer</strong> : Importe les signalements créés via l'application mobile depuis Firebase</li>
          <li><strong>Envoyer</strong> : Exporte les données locales (signalements et utilisateurs) vers Firebase pour l'affichage mobile</li>
          <li><strong>Synchronisation complète</strong> : Effectue les deux opérations dans l'ordre</li>
          <li>Les données sont automatiquement sauvegardées dans PostgreSQL local</li>
        </ul>
      </div>
    </div>
  );
}

export default Synchronization;
