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

  // Utilitaire générique pour appeler une API sync
  const callSyncApi = async (endpoint, label) => {
    if (!syncStatus.isOnline) {
      alert('❌ Pas de connexion Internet. Synchronisation impossible.');
      return;
    }
    setSyncing(true);
    addLog(`⏳ Appel de ${label}...`, 'info');
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/api/sync/${endpoint}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const data = await response.json();
      if (response.ok) {
        addLog(`✅ ${label} : ${data.message || 'Succès'} (${data.syncedCount !== undefined ? data.syncedCount : ''})`, 'success');
      } else {
        addLog(`❌ ${label} : ${data.message || 'Erreur'}`, 'error');
      }
      await fetchSyncStatus();
    } catch (error) {
      addLog(`❌ ${label} : Erreur réseau ou serveur`, 'error');
    } finally {
      setSyncing(false);
    }
  };

  useEffect(() => {
    checkConnectionStatus();
    fetchSyncStatus();
  }, []);

  const checkConnectionStatus = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/connectivity/firebase');
      // L'API retourne un booléen pur
      const isOnline = await response.json();
      setConnectionStatus(isOnline ? 'online' : 'offline');
      setSyncStatus(prev => ({ ...prev, isOnline }));
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

  const handleSyncBlockStatus = async () => {
    if (!syncStatus.isOnline) {
      alert('❌ Pas de connexion Internet. Synchronisation impossible.');
      return;
    }

    setSyncing(true);
    addLog('🔒 Synchronisation de l\'état de blocage depuis Firebase...', 'info');

    try {
      const token = localStorage.getItem('token');
      
      const response = await fetch('http://localhost:8080/api/sync/block-status', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!response.ok) throw new Error('Erreur synchronisation blocage');
      
      const data = await response.json();
      addLog(`✅ État de blocage synchronisé pour ${data.count} utilisateurs`, 'success');
      
      await fetchSyncStatus();
    } catch (error) {
      console.error('Erreur:', error);
      addLog(`❌ Erreur: ${error.message}`, 'error');
    } finally {
      setSyncing(false);
    }
  };

  const handleSyncDeblocage = async () => {
    if (!syncStatus.isOnline) {
      alert('❌ Pas de connexion Internet. Synchronisation impossible.');
      return;
    }

    setSyncing(true);
    addLog(`📤 Synchronisation de tous les déblocages vers Firebase...`, 'info');

    try {
      const token = localStorage.getItem('token');
      
      const response = await fetch(`http://localhost:8080/api/sync/deblocages`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!response.ok) throw new Error('Erreur synchronisation déblocage');
      
      const data = await response.json();
      addLog(`✅ ${data.count} utilisateurs synchronisés vers Firebase`, 'success');
      
      await fetchSyncStatus();
    } catch (error) {
      console.error('Erreur:', error);
      addLog(`❌ Erreur: ${error.message}`, 'error');
    } finally {
      setSyncing(false);
    }
  };

  const handleSyncImagesFromImgbb = async () => {
    if (!syncStatus.isOnline) {
      alert('❌ Pas de connexion Internet. Synchronisation impossible.');
      return;
    }

    setSyncing(true);
    addLog('📸 Démarrage de la synchronisation des images depuis imgbb...', 'info');

    try {
      const token = localStorage.getItem('token');
      
      const response = await fetch('http://localhost:8080/api/sync/sync-images-from-imgbb', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Erreur synchronisation images');
      }
      
      const data = await response.json();
      
      addLog(`✅ ${data.message}`, 'success');
      addLog(`📊 Trouvées: ${data.totalFound}, Téléchargées: ${data.downloaded}, Ignorées: ${data.skipped}, Échouées: ${data.failed}`, 'info');
      
      if (data.errors && data.errors.length > 0) {
        data.errors.forEach(error => {
          addLog(`⚠️ ${error}`, 'error');
        });
      }
      
      await fetchSyncStatus();
    } catch (error) {
      console.error('Erreur:', error);
      addLog(`❌ Erreur: ${error.message}`, 'error');
    } finally {
      setSyncing(false);
    }
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
          <div className="action-icon">📤</div>
          <h3>Synchroniser utilisateurs locaux → Firebase</h3>
          <p>POST /api/sync/users</p>
          <button className="action-btn" onClick={() => callSyncApi('users', 'Utilisateurs locaux → Firebase')} disabled={syncing || !syncStatus.isOnline}>
            {syncing ? '⏳...' : 'Synchroniser'}
          </button>
        </div>
        <div className="action-card">
          <div className="action-icon">📥</div>
          <h3>Synchroniser utilisateurs Firebase → PostgreSQL</h3>
          <p>POST /api/sync/users/from-firebase</p>
          <button className="action-btn" onClick={() => callSyncApi('users/from-firebase', 'Utilisateurs Firebase → PostgreSQL')} disabled={syncing || !syncStatus.isOnline}>
            {syncing ? '⏳...' : 'Synchroniser'}
          </button>
        </div>
        <div className="action-card">
          <div className="action-icon">📝</div>
          <h3>Synchroniser modifications hors ligne</h3>
          <p>POST /api/sync/users/offline-changes</p>
          <button className="action-btn" onClick={() => callSyncApi('users/offline-changes', 'Modifications hors ligne')} disabled={syncing || !syncStatus.isOnline}>
            {syncing ? '⏳...' : 'Synchroniser'}
          </button>
        </div>
        <div className="action-card">
          <div className="action-icon">🔖</div>
          <h3>Synchroniser métadonnées</h3>
          <p>POST /api/sync/metadata</p>
          <button className="action-btn" onClick={() => callSyncApi('metadata', 'Métadonnées')} disabled={syncing || !syncStatus.isOnline}>
            {syncing ? '⏳...' : 'Synchroniser'}
          </button>
        </div>
        <div className="action-card">
          <div className="action-icon">🔄</div>
          <h3>Synchronisation bi-directionnelle des signalements</h3>
          <p>POST /api/sync/signalements/bidirectional</p>
          <button className="action-btn" onClick={() => callSyncApi('signalements/bidirectional', 'Sync bi-directionnelle signalements')} disabled={syncing || !syncStatus.isOnline}>
            {syncing ? '⏳...' : 'Synchroniser'}
          </button>
        </div>

        <div className="action-card">
          <div className="action-icon">🔒</div>
          <h3>Synchroniser l'état de blocage</h3>
          <p>Importer l'état de blocage depuis Firebase (source de vérité)</p>
          <button 
            className="action-btn warning"
            onClick={handleSyncBlockStatus}
            disabled={syncing || !syncStatus.isOnline}
          >
            {syncing ? '⏳ Synchronisation...' : '🔒 Synchroniser blocage'}
          </button>
        </div>

        <div className="action-card">
          <div className="action-icon">📤</div>
          <h3>Synchroniser le déblocage</h3>
          <p>Envoyer l'état de déblocage PostgreSQL vers Firebase</p>
          <button 
            className="action-btn info"
            onClick={handleSyncDeblocage}
            disabled={syncing || !syncStatus.isOnline}
          >
            {syncing ? '⏳ Synchronisation...' : '📤 Synchroniser déblocage'}
          </button>
        </div>

        <div className="action-card">
          <div className="action-icon">📸</div>
          <h3>Télécharger images imgbb (manuel)</h3>
          <p>Force le téléchargement de toutes les images imgbb vers local</p>
          <button 
            className="action-btn warning"
            onClick={handleSyncImagesFromImgbb}
            disabled={syncing || !syncStatus.isOnline}
          >
            {syncing ? '⏳ Téléchargement...' : '📸 Télécharger toutes images'}
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
          <li><strong>Synchroniser l'état de blocage</strong> : Importe l'état de blocage des comptes depuis Firebase (qui est la source de vérité) vers PostgreSQL. Les données incluent: état de blocage, nombre de tentatives échouées et date de la dernière tentative</li>
          <li><strong>📸 Images automatiques</strong> : Les images des signalements sont automatiquement téléchargées depuis imgbb vers le stockage local lors de la synchronisation des signalements</li>
          <li>Les données sont automatiquement sauvegardées dans PostgreSQL local</li>
        </ul>
      </div>
    </div>
  );
}

export default Synchronization;
