import React, { useEffect, useState, useRef } from 'react';
import { fetchWithAuth } from '../services/authService';
import { FaCheckCircle, FaTimes, FaPlus, FaLock, FaUnlock, FaKey, FaUsers, FaMobileAlt, FaExclamationTriangle, FaCalendar, FaBolt, FaUpload, FaDownload, FaSync, FaClipboardList, FaTerminal } from 'react-icons/fa';
import './UserManagement.css';
import './sync-terminal.css';
import { auth, googleProvider } from '../firebase';
import { signInWithPopup } from 'firebase/auth';

function UserManagement() {
  const [users, setUsers] = useState([]);
  const [blockedUsers, setBlockedUsers] = useState([]);
  const [allDeblocages, setAllDeblocages] = useState([]);
  const [filterType, setFilterType] = useState('blocked');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [selectedUserHistory, setSelectedUserHistory] = useState(null);
  const [showConfigModal, setShowConfigModal] = useState(false);
  const [configForm, setConfigForm] = useState({
    tentativesMax: 3,
    dureeSessionMinutes: 1440,
    dureeBloquageMinutes: 30
  });
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [sources, setSources] = useState([]);
  const [statuses, setStatuses] = useState([]);
  const [newUser, setNewUser] = useState({
    nom: '',
    prenom: '',
    email: '',
    numTel: '',
    password: ''
  });
  const [syncLog, setSyncLog] = useState([]);
  const terminalBodyRef = useRef(null);

  // Auto-scroll vers le bas du terminal
  useEffect(() => {
    if (terminalBodyRef.current) {
      terminalBodyRef.current.scrollTop = terminalBodyRef.current.scrollHeight;
    }
  }, [syncLog, loading]);

  useEffect(() => {
    fetchUsers();
    fetchConfiguration();
    fetchSources();
    fetchStatuses();
  }, []);

  const fetchConfiguration = async () => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch('http://localhost:8080/api/configurations', {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (res.ok) {
        const configs = await res.json();
        if (Array.isArray(configs)) {
          const configMap = {};
          configs.forEach(config => {
            if (config.cle === 'tentatives_max') {
              configMap.tentativesMax = parseInt(config.valeur) || 3;
            } else if (config.cle === 'duree_session_minutes') {
              configMap.dureeSessionMinutes = parseInt(config.valeur) || 1440;
            } else if (config.cle === 'duree_blocage_minutes') {
              configMap.dureeBloquageMinutes = parseInt(config.valeur) || 30;
            }
          });
          setConfigForm(prev => ({ ...prev, ...configMap }));
        }
      }
    } catch (error) {
      console.error('Erreur lors du chargement des configurations:', error);
    }
  };

  const fetchSources = async () => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch('http://localhost:8080/api/sources', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        setSources(data);
      } else {
        console.error('Erreur chargement sources:', res.status);
      }
    } catch (error) {
      console.error('Erreur lors du chargement des sources:', error);
    }
  };

  const fetchStatuses = async () => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch('http://localhost:8080/api/status', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        setStatuses(data);
      } else {
        console.error('Erreur chargement statuts:', res.status);
      }
    } catch (error) {
      console.error('Erreur lors du chargement des statuts:', error);
    }
  };

  const fetchUsers = async () => {
    try {
      const token = localStorage.getItem('token');

      if (!token) {
        setError("Non connecté : connecte-toi pour accéder aux utilisateurs.");
        setUsers([]);
        setBlockedUsers([]);
        return;
      }
      
      const [allUsersRes, blockedUsersRes, deblocagesRes] = await Promise.all([
        fetchWithAuth('http://localhost:8080/api/users'),
        fetchWithAuth('http://localhost:8080/api/users/blocked'),
        fetchWithAuth('http://localhost:8080/api/deblocages')
      ]);

      if (!allUsersRes.ok || !blockedUsersRes.ok) {
        const status = !allUsersRes.ok ? allUsersRes.status : blockedUsersRes.status;
        setError(
          status === 401 || status === 403
            ? "Accès refusé (token invalide/expiré). Reconnecte-toi."
            : `Erreur API (HTTP ${status}) lors du chargement des utilisateurs.`
        );
        setUsers([]);
        setBlockedUsers([]);
        return;
      }

      const allUsers = await allUsersRes.json();
      const blocked = await blockedUsersRes.json();
      const deblocages = deblocagesRes.ok ? await deblocagesRes.json() : [];

      setError('');
      setUsers(Array.isArray(allUsers) ? allUsers : []);
      setBlockedUsers(Array.isArray(blocked) ? blocked : []);
      setAllDeblocages(Array.isArray(deblocages) ? deblocages : []);
    } catch (error) {
      console.error('Erreur:', error);
      setError("Impossible de contacter l'API (backend arrêté ou CORS).");
      setUsers([]);
      setBlockedUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const handleUnblock = async (userId) => {
    const token = localStorage.getItem('token');
    const currentUser = users.find(u => u.id === userId);
    
    if (!token) {
      alert('Non connecté. Reconnecte-toi.');
      return;
    }

    const motif = prompt(`Motif du déblocage pour ${currentUser?.prenom} ${currentUser?.nom}:`, 'Déblocage manuel');
    
    if (motif === null) {
      return; // Utilisateur a annulé
    }

    try {
      setLoading(true);
      const res = await fetch(`http://localhost:8080/api/deblocages/debloquer/${userId}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          idManager: parseInt(localStorage.getItem('userId')) || 1,
          motif: motif || 'Déblocage manuel'
        })
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || `Erreur HTTP ${res.status}`);
      }

      alert('Utilisateur débloqué avec succès');
      await fetchUsers(); // Rafraîchir la liste
    } catch (error) {
      console.error('Erreur:', error);
      alert(`Erreur lors du déblocage: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (userId) => {
    alert('⚠️ Fonctionnalité non disponible\n\nL\'endpoint de réinitialisation de mot de passe n\'existe pas encore dans le backend.\n\nVeuillez contacter l\'administrateur pour implémenter cette fonctionnalité.');
  };

  const handleCreateGoogleAccount = async () => {
    try {
      // Ouvrir popup Google Sign-In
      const result = await signInWithPopup(auth, googleProvider);
      const user = result.user;
      const idToken = await user.getIdToken();

      // Appeler l'API back avec le token
      const token = localStorage.getItem('token');
      const response = await fetch('http://localhost:8080/api/auth/register-google', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          idToken: idToken,
          nom: user.displayName?.split(' ')[0] || '',
          prenom: user.displayName?.split(' ')[1] || ''
        })
      });

      if (!response.ok) {
        const errorText = await response.text();
        console.error('Response status:', response.status, 'Body:', errorText);
        throw new Error(errorText || 'Erreur lors de la création');
      }

      const responseText = await response.text();
      console.log('Response text:', responseText);
      const newUser = responseText ? JSON.parse(responseText) : {};
      alert(`Compte Google créé avec succès pour ${newUser.email}`);
      await fetchUsers(); // Rafraîchir la liste
    } catch (error) {
      console.error('Erreur:', error);
      alert('Erreur lors de la création du compte Google: ' + error.message);
    }
  };

  const handleCreateUser = async () => {
    // Validation
    if (!newUser.nom.trim() || !newUser.prenom.trim() || !newUser.email.trim() || !newUser.password.trim()) {
      alert('Veuillez remplir tous les champs obligatoires (nom, prénom, email, mot de passe)');
      return;
    }

    try {
      const token = localStorage.getItem('token');
      const userData = {
        nom: newUser.nom.trim(),
        prenom: newUser.prenom.trim(),
        email: newUser.email.trim(),
        numTel: newUser.numTel.trim() || null,
        password: newUser.password.trim()
      };

      const response = await fetch('http://localhost:8080/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(userData)
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Erreur lors de la création');
      }

      alert('Utilisateur créé avec succès');
      setShowCreateModal(false);
      setNewUser({
        nom: '',
        prenom: '',
        email: '',
        numTel: '',
        password: ''
      });
      await fetchUsers();
    } catch (error) {
      console.error('Erreur:', error);
      alert(`Erreur lors de la création : ${error.message}`);
    }
  };

  // Utility function to format dates
  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  };

  const getStatusBadge = (user) => {
    if (user.deletedAt || user.deleteLe) {
      return <span className="status-badge status-deleted">Supprimé</span>;
    }
    if ((user.tentativesConnexion || user.tentatives || 0) >= 3) {
      return <span className="status-badge status-blocked">Bloqué</span>;
    }
    return <span className="status-badge status-active">Actif</span>;
  };

  const displayedUsers = filterType === 'blocked' ? blockedUsers : users;

  const getDeblocageCountForUser = (userId) => {
    return allDeblocages.filter(d => d.idUtilisateur === userId || d.utilisateur?.id === userId).length;
  };

  const getUserHistory = (userId) => {
    return allDeblocages.filter(d => d.idUtilisateur === userId || d.utilisateur?.id === userId);
  };

  const handleSaveConfiguration = async () => {
    try {
      const token = localStorage.getItem('token');
      const configs = [
        {
          cle: 'tentatives_max',
          valeur: configForm.tentativesMax.toString(),
          description: 'Nombre maximum de tentatives de connexion avant blocage'
        },
        {
          cle: 'duree_session_minutes',
          valeur: configForm.dureeSessionMinutes.toString(),
          description: 'Durée de validité d\'une session en minutes'
        },
        {
          cle: 'duree_blocage_minutes',
          valeur: configForm.dureeBloquageMinutes.toString(),
          description: 'Durée du blocage après trop de tentatives'
        }
      ];

      for (const config of configs) {
        const res = await fetch('http://localhost:8080/api/configurations', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify(config)
        });

        if (!res.ok) {
          throw new Error('Erreur lors de la sauvegarde');
        }
      }

      alert('Configuration sauvegardée avec succès');
      setShowConfigModal(false);
    } catch (error) {
      console.error('Erreur:', error);
      alert('Erreur lors de la sauvegarde de la configuration');
    }
  };

  // ===== SYNCHRONISATION UTILISATEURS =====
  const addLog = (message, type = 'info', options = {}) => {
    const timestamp = new Date().toLocaleTimeString('fr-FR');
    const entry = { 
      message, 
      type, 
      timestamp,
      badge: options.badge || null,
      icon: options.icon || null
    };
    setSyncLog(prev => [...prev, entry]);
  };

  const addSessionSeparator = (sessionName) => {
    const timestamp = new Date().toLocaleTimeString('fr-FR');
    setSyncLog(prev => [...prev, { 
      type: 'session', 
      message: sessionName,
      timestamp,
      separator: true
    }]);
  };

  const clearLog = () => {
    setSyncLog([]);
  };

  const handleSyncUsersToFirebase = async () => {
    if (!window.confirm('Synchroniser les utilisateurs locaux vers Firebase?\n\nCette opération enverra tous les utilisateurs non synchronisés vers Firebase.')) {
      return;
    }

    try {
      setLoading(true);
      addSessionSeparator('POSTGRESQL → FIREBASE');
      addLog('Démarrage de la synchronisation...', 'info');
      
      const token = localStorage.getItem('token');
      const res = await fetch('http://localhost:8080/api/sync/users', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.message || `Erreur HTTP ${res.status}`);
      }

      addLog('Synchronisation réussie!', 'success', { 
        badge: `${data.count} utilisateurs`,
        icon: '✅' 
      });
    } catch (error) {
      console.error('Erreur:', error);
      addLog(`Erreur: ${error.message}`, 'error', { icon: '❌' });
    } finally {
      setLoading(false);
    }
  };

  const handleSyncUsersFromFirebase = async () => {
    if (!window.confirm('Synchroniser les utilisateurs depuis Firebase vers PostgreSQL?\n\nCette opération récupérera tous les utilisateurs Firebase non synchronisés.')) {
      return;
    }

    try {
      setLoading(true);
      addSessionSeparator('FIREBASE → POSTGRESQL');
      addLog('Récupération des utilisateurs...', 'info');
      
      const token = localStorage.getItem('token');
      const res = await fetch('http://localhost:8080/api/sync/users/from-firebase', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.message || `Erreur HTTP ${res.status}`);
      }

      addLog('Synchronisation réussie!', 'success', { 
        badge: `${data.count} utilisateurs`,
        icon: '✅' 
      });
      await fetchUsers(); // Rafraîchir la liste
      addLog('Liste des utilisateurs rechargée', 'success');
    } catch (error) {
      console.error('Erreur:', error);
      addLog(`Erreur: ${error.message}`, 'error', { icon: '❌' });
    } finally {
      setLoading(false);
    }
  };

  // ===== SYNCHRONISATION ÉTAT DE BLOCAGE =====
  const handleSyncBlockStatus = async () => {
    if (!window.confirm('Synchroniser l\'état de blocage depuis Firebase?\n\nCette opération mettra à jour l\'état de blocage depuis Firebase (source de vérité).')) {
      return;
    }

    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      const res = await fetch('http://localhost:8080/api/sync/block-status', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.message || `Erreur HTTP ${res.status}`);
      }

      addLog('Synchronisation réussie!', 'success', { 
        badge: `${data.count} utilisateurs`,
        icon: '✅' 
      });
      await fetchUsers(); // Rafraîchir la liste
    } catch (error) {
      console.error('Erreur:', error);
      addLog(`Erreur: ${error.message}`, 'error', { icon: '❌' });
    } finally {
      setLoading(false);
    }
  };

  const handleSyncDeblocages = async () => {
    if (!window.confirm('Synchroniser tous les déblocages vers Firebase?\n\nCette opération mettra à jour l\'état de déblocage de tous les utilisateurs.')) {
      return;
    }

    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      const res = await fetch('http://localhost:8080/api/sync/deblocages', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.message || `Erreur HTTP ${res.status}`);
      }

      addLog('Synchronisation réussie!', 'success', { 
        badge: `${data.count} utilisateurs`,
        icon: '✅' 
      });
    } catch (error) {
      console.error('Erreur:', error);
      addLog(`Erreur: ${error.message}`, 'error', { icon: '❌' });
    } finally {
      setLoading(false);
    }
  };

  if (error) {
    return (
      <div className="user-management">
        <header className="page-header">
          <h1>Gestion des Utilisateurs</h1>
          <p>{error}</p>
        </header>
      </div>
    );
  }

  return (
    <div className="user-management">
      <header className="page-header">
        <div>
          <h1>Gestion des Utilisateurs</h1>
          <p>Débloquer les comptes et gérer les utilisateurs</p>
          {loading && (
            <div className="inline-loading">
              <div className="loading-spinner"></div>
              <span>Chargement...</span>
            </div>
          )}
        </div>
        <div className="header-buttons">
          <button
            className="btn-create"
            onClick={() => setShowCreateModal(true)}
            title="Créer un nouvel utilisateur"
          >
            <FaPlus /> Créer un utilisateur
          </button>
          <button
            className="btn-config"
            onClick={() => setShowConfigModal(true)}
            title="Gérer la configuration système"
          >
             Configuration
          </button>
        </div>
      </header>

      <div className="stats-cards">
        <div className="stat-card">
          <div className="stat-icon"><FaUsers /></div>
          <div className="stat-content">
            <div className="stat-value">{users.length}</div>
            <div className="stat-label">Total utilisateurs</div>
          </div>
        </div>

        <div className="stat-card danger">
          <div className="stat-icon"><FaLock /></div>
          <div className="stat-content">
            <div className="stat-value">{blockedUsers.length}</div>
            <div className="stat-label">Utilisateurs bloqués</div>
          </div>
        </div>
      </div>

      {/* Synchronisation Section */}
      <div className="sync-section">
        <div className="sync-header">
          <h3><FaSync /> Synchronisation Firebase</h3>
          <p>Gérer la synchronisation bidirectionnelle des données</p>
        </div>
        
        <div className="sync-content">
          <div className="sync-buttons">
            <button className="btn-sync primary" onClick={handleSyncUsersToFirebase} title="PostgreSQL → Firebase">
              <FaUpload /> Vers Firebase
            </button>
            <button className="btn-sync secondary" onClick={handleSyncUsersFromFirebase} title="Firebase → PostgreSQL">
              <FaDownload /> Depuis Firebase
            </button>
            <button className="btn-sync danger" onClick={handleSyncBlockStatus} title="Synchroniser l'état de blocage">
              <FaLock /> État blocage
            </button>
            <button className="btn-sync success" onClick={handleSyncDeblocages} title="Synchroniser les déblocages">
              <FaUnlock /> Déblocages
            </button>
          </div>

          <div className="sync-terminal">
            <div className="sync-terminal-header">
              <span><FaTerminal /> Logs</span>
              <button className="sync-terminal-clear" onClick={clearLog} title="Effacer">
                <FaTimes />
              </button>
            </div>
            <div className="sync-terminal-body" ref={terminalBodyRef}>
              {syncLog.length === 0 ? (
                <div className="sync-terminal-empty">En attente...</div>
              ) : (
                syncLog.map((log, index) => (
                  <React.Fragment key={index}>
                    {log.type === 'session' ? (
                      <div className="sync-terminal-separator">{log.message}</div>
                    ) : (
                      <div className={`sync-terminal-line sync-${log.type}`}>
                        <span className="sync-time">{log.timestamp}</span>
                        <span className="sync-icon">
                          {log.icon || (
                            <>
                              {log.type === 'success' && '✓'}
                              {log.type === 'error' && '✗'}
                              {log.type === 'info' && '→'}
                            </>
                          )}
                        </span>
                        <span className="sync-message-text">
                          {log.message}
                          {log.badge && <span className="sync-badge">{log.badge}</span>}
                        </span>
                      </div>
                    )}
                  </React.Fragment>
                ))
              )}
              {loading && (
                <div className="sync-terminal-line sync-loading">
                  <span className="sync-icon">⟳</span>
                  <span className="sync-message-text">En cours...</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="filter-section">
        <button
          className={`filter-btn ${filterType === 'blocked' ? 'active' : ''}`}
          onClick={() => setFilterType('blocked')}
        >
           Utilisateurs bloqués ({blockedUsers.length})
        </button>
        <button
          className={`filter-btn ${filterType === 'all' ? 'active' : ''}`}
          onClick={() => setFilterType('all')}
        >
           Tous les utilisateurs ({users.length})
        </button>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Utilisateur</th>
              <th>Email</th>
              <th><FaMobileAlt /> Téléphone</th>
              <th>Statut</th>
              <th><FaExclamationTriangle /> Tentatives</th>
              <th><FaCalendar /> Créé le</th>
              <th>Mis à jour</th>
              <th><FaBolt /> Actions</th>
            </tr>
          </thead>
          <tbody>
            {displayedUsers.map((user) => {
              const tentatives = user.tentativesConnexion || user.tentatives || 0;
              return (
              <tr key={user.id} className={tentatives >= 3 ? 'blocked-row' : ''}>
                <td>
                  <div className="user-cell">
                    <div className="user-avatar">
                      {user.prenom?.[0]?.toUpperCase()}{user.nom?.[0]?.toUpperCase()}
                    </div>
                    <div className="user-info">
                      <div className="user-name">{user.prenom} {user.nom}</div>
                      <div className="user-id">ID: #{user.id}</div>
                    </div>
                  </div>
                </td>
                <td>
                  <div className="email-cell">{user.email}</div>
                </td>
                <td>{user.numTel || 'N/A'}</td>
                <td>{getStatusBadge(user)}</td>
                <td>
                  <div className="attempts-cell">
                    <span className={`attempts-badge ${tentatives >= 3 ? 'max-attempts' : ''}`}>
                      {tentatives} / 3
                    </span>
                    <div className="attempts-bar">
                      <div 
                        className="attempts-progress" 
                        style={{ width: `${(tentatives / 3) * 100}%` }}
                      ></div>
                    </div>
                  </div>
                </td>
                <td>
                  <div className="date-cell">{formatDate(user.createdAt || user.creeLe)}</div>
                </td>
                <td>
                  <div className="date-cell">{formatDate(user.updatedAt || user.updateLe)}</div>
                </td>
                <td>
                  <div className="action-buttons">
                    {(filterType === 'blocked') && (
                      <button
                        className="btn-action btn-unblock"
                        onClick={() => handleUnblock(user.id)}
                        title="Débloquer l'utilisateur"
                      >
                        <FaUnlock />
                      </button>
                    )}
                    <button
                      className="btn-action btn-history"
                      onClick={() => {
                        const userHistory = getUserHistory(user.id);
                        setSelectedUserHistory({
                          user,
                          history: userHistory
                        });
                        setShowHistoryModal(true);
                      }}
                      title={`Voir l'historique (${getDeblocageCountForUser(user.id)} déblocages)`}
                    >
                      <FaClipboardList />
                    </button>
                    <button
                      className="btn-action btn-reset"
                      onClick={() => handleResetPassword(user.id)}
                      title="Réinitialiser le mot de passe"
                    >
                      <FaKey />
                    </button>
                  </div>
                </td>
              </tr>
              );
            })}
          </tbody>
        </table>

        {displayedUsers.length === 0 && (
          <div className="no-results">
            {filterType === 'blocked' 
              ? ' Aucun utilisateur bloqué !' 
              : 'Aucun utilisateur trouvé'}
          </div>
        )}
      </div>

      <div className="info-box">
        <h3> Informations</h3>
        <ul>
          <li>Les utilisateurs sont automatiquement bloqués après <strong>{configForm.tentativesMax} tentatives</strong> de connexion échouées</li>
          <li>Utilisez le bouton "Débloquer" pour réinitialiser le compteur de tentatives</li>
          <li>Le bouton "Historique" affiche le nombre de déblocages et les dates</li>
          <li>Le bouton "Reset" génère un mot de passe temporaire pour l'utilisateur</li>
          <li>Les utilisateurs peuvent se débloquer après un certain délai (configurable dans les paramètres)</li>
        </ul>
        <button
          className="btn-create-google"
          onClick={handleCreateGoogleAccount}
          title="Créer un compte Google"
        >
          <FaPlus /> Créer compte Google
        </button>
      </div>

      {/* Modal Historique */}
      {showHistoryModal && selectedUserHistory && (
        <div className="modal-overlay" onClick={() => setShowHistoryModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2> Historique des déblocages</h2>
              <button className="modal-close" onClick={() => setShowHistoryModal(false)}>✕</button>
            </div>

            <div className="modal-body">
              <div className="user-info-modal">
                <h3>{selectedUserHistory.user.prenom} {selectedUserHistory.user.nom}</h3>
                <p>Email: <strong>{selectedUserHistory.user.email}</strong></p>
                <p>Nombre total de déblocages: <strong className="blockage-count">{selectedUserHistory.history.length}</strong></p>
              </div>

              {selectedUserHistory.history.length > 0 ? (
                <div className="history-table">
                  <table>
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Manager</th>
                        <th>Motif</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedUserHistory.history.map((record, index) => (
                        <tr key={index}>
                          <td>{formatDate(record.createdAt)}</td>
                          <td>
                            {record.manager ? `${record.manager.prenom} ${record.manager.nom}` : 'N/A'}
                          </td>
                          <td>{record.motif || 'Aucun motif'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="no-history">Aucun déblocage enregistré pour cet utilisateur</p>
              )}
            </div>

            <div className="modal-footer">
              <button className="btn-close-modal" onClick={() => setShowHistoryModal(false)}>Fermer</button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Configuration */}
      {showConfigModal && (
        <div className="modal-overlay" onClick={() => setShowConfigModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2> Gestion de Configuration</h2>
              <button className="modal-close" onClick={() => setShowConfigModal(false)}>✕</button>
            </div>

            <div className="modal-body">
              <div className="config-form">
                <div className="form-group">
                  <label htmlFor="tentativesMax">Nombre maximum de tentatives de connexion</label>
                  <input
                    type="number"
                    id="tentativesMax"
                    min="1"
                    max="10"
                    value={configForm.tentativesMax}
                    onChange={(e) => setConfigForm({ ...configForm, tentativesMax: parseInt(e.target.value) || 3 })}
                  />
                  <small>Après ce nombre de tentatives échouées, le compte sera bloqué</small>
                </div>

                <div className="form-group">
                  <label htmlFor="dureeSessionMinutes">Durée de session (minutes)</label>
                  <input
                    type="number"
                    id="dureeSessionMinutes"
                    min="30"
                    max="1440"
                    step="30"
                    value={configForm.dureeSessionMinutes}
                    onChange={(e) => setConfigForm({ ...configForm, dureeSessionMinutes: parseInt(e.target.value) || 1440 })}
                  />
                  <small>Durée de validité d'une session utilisateur en minutes (défaut: 24 heures)</small>
                </div>

                <div className="form-group">
                  <label htmlFor="dureeBloquageMinutes">Durée de blocage (minutes)</label>
                  <input
                    type="number"
                    id="dureeBloquageMinutes"
                    min="5"
                    max="1440"
                    step="5"
                    value={configForm.dureeBloquageMinutes}
                    onChange={(e) => setConfigForm({ ...configForm, dureeBloquageMinutes: parseInt(e.target.value) || 30 })}
                  />
                  <small>Durée pendant laquelle un compte reste bloqué après trop de tentatives</small>
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button 
                className="btn-save-config" 
                onClick={handleSaveConfiguration}
              >
                 Sauvegarder
              </button>
              <button 
                className="btn-close-modal" 
                onClick={() => setShowConfigModal(false)}
              >
                Annuler
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de création d'utilisateur */}
      {showCreateModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h2>Créer un nouvel utilisateur</h2>
              <button 
                className="btn-close-modal" 
                onClick={() => setShowCreateModal(false)}
              >
                ✕
              </button>
            </div>

            <div className="modal-body">
              <div className="user-form">
                <div className="form-group">
                  <label htmlFor="nom">Nom *</label>
                  <input
                    type="text"
                    id="nom"
                    value={newUser.nom}
                    onChange={(e) => setNewUser({ ...newUser, nom: e.target.value })}
                    placeholder="Entrez le nom"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="prenom">Prénom *</label>
                  <input
                    type="text"
                    id="prenom"
                    value={newUser.prenom}
                    onChange={(e) => setNewUser({ ...newUser, prenom: e.target.value })}
                    placeholder="Entrez le prénom"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="email">Email *</label>
                  <input
                    type="email"
                    id="email"
                    value={newUser.email}
                    onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
                    placeholder="Entrez l'email"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="numTel">Numéro de téléphone</label>
                  <input
                    type="tel"
                    id="numTel"
                    value={newUser.numTel}
                    onChange={(e) => setNewUser({ ...newUser, numTel: e.target.value })}
                    placeholder="Entrez le numéro de téléphone"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="password">Mot de passe *</label>
                  <input
                    type="password"
                    id="password"
                    value={newUser.password}
                    onChange={(e) => setNewUser({ ...newUser, password: e.target.value })}
                    placeholder="Entrez le mot de passe"
                  />
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button 
                className="btn-save-config" 
                onClick={handleCreateUser}
              >
                Créer l'utilisateur
              </button>
              <button 
                className="btn-close-modal" 
                onClick={() => setShowCreateModal(false)}
              >
                Annuler
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default UserManagement;
