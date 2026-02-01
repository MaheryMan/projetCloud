import React, { useEffect, useState } from 'react';
import './UserManagement.css';
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

  useEffect(() => {
    fetchUsers();
    fetchConfiguration();
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
        fetch('http://localhost:8080/api/users', {
          headers: { 'Authorization': `Bearer ${token}` }
        }),
        fetch('http://localhost:8080/api/users/blocked', {
          headers: { 'Authorization': `Bearer ${token}` }
        }),
        fetch('http://localhost:8080/api/deblocages', {
          headers: { 'Authorization': `Bearer ${token}` }
        })
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
    if (!window.confirm('Débloquer cet utilisateur ?')) {
      return;
    }

    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/api/users/${userId}/unblock`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}` 
        },
        body: JSON.stringify({
          createdAt: new Date().toISOString()
        })
      });

      if (!response.ok) throw new Error('Erreur de déblocage');

      alert('Utilisateur débloqué avec succès');
      await fetchUsers();
    } catch (error) {
      console.error('Erreur:', error);
      alert('Erreur lors du déblocage');
    }
  };

  const handleResetPassword = async (userId) => {
    if (!window.confirm('Réinitialiser le mot de passe de cet utilisateur ?')) {
      return;
    }

    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/api/users/${userId}/reset-password`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!response.ok) throw new Error('Erreur de réinitialisation');

      const data = await response.json();
      alert(`Mot de passe réinitialisé : ${data.temporaryPassword}`);
    } catch (error) {
      console.error('Erreur:', error);
      alert('Erreur lors de la réinitialisation');
    }
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

  const formatDate = (dateString) => {
    if (!dateString) {
      return new Date().toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    }
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  };

  const getStatusBadge = (user) => {
    if (user.deleteLe) {
      return <span className="status-badge status-deleted">Supprimé</span>;
    }
    if (user.tentatives >= 3) {
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

  if (loading) {
    return <div className="loading">Chargement...</div>;
  }

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
        </div>
        <button
          className="btn-config"
          onClick={() => setShowConfigModal(true)}
          title="Gérer la configuration système"
        >
           Configuration
        </button>
      </header>

      <div className="stats-cards">
        <div className="stat-card">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <div className="stat-value">{users.length}</div>
            <div className="stat-label">Total utilisateurs</div>
          </div>
        </div>

        <div className="stat-card danger">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <div className="stat-value">{blockedUsers.length}</div>
            <div className="stat-label">Utilisateurs bloqués</div>
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
              <th>ID</th>
              <th>Nom complet</th>
              <th>Email</th>
              <th>Téléphone</th>
              <th>Statut</th>
              <th>Tentatives</th>
              <th>Créé le</th>
              <th>Dernière mise à jour</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {displayedUsers.map((user) => (
              <tr key={user.id} className={user.tentatives >= 3 ? 'blocked-row' : ''}>
                <td>#{user.id}</td>
                <td>
                  <div className="user-name">
                    <strong>{user.prenom} {user.nom}</strong>
                  </div>
                </td>
                <td>{user.email}</td>
                <td>{user.numTel || 'N/A'}</td>
                <td>{getStatusBadge(user)}</td>
                <td>
                  <span className={`attempts ${user.tentatives >= 3 ? 'max-attempts' : ''}`}>
                    {user.tentatives} / 3
                  </span>
                </td>
                <td>{formatDate(user.creeLe)}</td>
                <td>{formatDate(user.updateLe)}</td>
                <td>
                  <div className="action-buttons">
                    {(filterType === 'blocked') && (
                      <button
                        className="btn-unblock"
                        onClick={() => handleUnblock(user.id)}
                        title="Débloquer l'utilisateur"
                      >
                         Débloquer
                      </button>
                    )}
                    <button
                      className="btn-history"
                      onClick={() => {
                        const userHistory = getUserHistory(user.id);
                        setSelectedUserHistory({
                          user,
                          history: userHistory
                        });
                        setShowHistoryModal(true);
                      }}
                      title="Voir l'historique des déblocages"
                    >
                       Historique ({getDeblocageCountForUser(user.id)})
                    </button>
                    <button
                      className="btn-reset"
                      onClick={() => handleResetPassword(user.id)}
                      title="Réinitialiser le mot de passe"
                    >
                       Reset
                    </button>
                  </div>
                </td>
              </tr>
            ))}
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
          ➕ Créer compte Google
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
    </div>
  );
}

export default UserManagement;
