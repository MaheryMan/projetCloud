import React, { useEffect, useState } from 'react';
import './UserManagement.css';

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
    idSource: '',
    idStatus: '',
    password: '',
    confirmPassword: ''
  });

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

  const handleCreateUser = async () => {
    // Validation
    if (!newUser.nom.trim() || !newUser.prenom.trim() || !newUser.email.trim() || !newUser.idSource || !newUser.idStatus) {
      alert('Veuillez remplir tous les champs obligatoires');
      return;
    }

    // Validation du mot de passe pour les utilisateurs locaux
    const selectedSource = sources.find(s => s.id == newUser.idSource);
    if (selectedSource?.providerType === 'local') {
      if (!newUser.password.trim()) {
        alert('Le nouveau mot de passe est obligatoire pour les utilisateurs locaux');
        return;
      }
      if (!newUser.confirmPassword.trim()) {
        alert('La confirmation du mot de passe est obligatoire');
        return;
      }
      if (newUser.password !== newUser.confirmPassword) {
        alert('Les mots de passe ne correspondent pas');
        return;
      }
    }

    try {
      const token = localStorage.getItem('token');
      const userData = {
        nom: newUser.nom.trim(),
        prenom: newUser.prenom.trim(),
        email: newUser.email.trim(),
        numTel: newUser.numTel.trim() || null,
        idSource: parseInt(newUser.idSource),
        idStatus: parseInt(newUser.idStatus)
      };

      // Ajouter le mot de passe seulement pour les utilisateurs locaux
      if (selectedSource?.providerType === 'local') {
        userData.password = newUser.password.trim();
      }

      const response = await fetch('http://localhost:8080/api/users', {
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
        idSource: '',
        idStatus: '',
        password: '',
        confirmPassword: ''
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
        <div className="header-buttons">
          <button
            className="btn-create"
            onClick={() => setShowCreateModal(true)}
            title="Créer un nouvel utilisateur"
          >
            ➕ Créer un utilisateur
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
                  <label htmlFor="idSource">Source *</label>
                  <select
                    id="idSource"
                    value={newUser.idSource}
                    onChange={(e) => setNewUser({ ...newUser, idSource: e.target.value })}
                    style={{ color: '#2c3e50', backgroundColor: 'white' }}
                  >
                    <option value="" style={{ color: '#2c3e50', backgroundColor: 'white' }}>Sélectionnez une source</option>
                    {sources.map(source => (
                      <option key={source.id} value={source.id} style={{ color: '#2c3e50', backgroundColor: 'white' }}>
                        {source.libelle}
                      </option>
                    ))}
                  </select>
                 
                </div>

                {sources.find(s => s.id == newUser.idSource)?.providerType === 'local' && (
                  <>
                    <div className="form-group">
                      <label htmlFor="password">Nouveau mot de passe *</label>
                      <input
                        type="password"
                        id="password"
                        value={newUser.password}
                        onChange={(e) => setNewUser({ ...newUser, password: e.target.value })}
                        placeholder="Entrez le nouveau mot de passe"
                      />
                    </div>

                    <div className="form-group">
                      <label htmlFor="confirmPassword">Confirmer mot de passe *</label>
                      <input
                        type="password"
                        id="confirmPassword"
                        value={newUser.confirmPassword}
                        onChange={(e) => setNewUser({ ...newUser, confirmPassword: e.target.value })}
                        placeholder="Confirmez le mot de passe"
                      />
                      {newUser.password && newUser.confirmPassword && newUser.password !== newUser.confirmPassword && (
                        <small style={{ color: '#e74c3c' }}>Les mots de passe ne correspondent pas</small>
                      )}
                      {newUser.password && newUser.confirmPassword && newUser.password === newUser.confirmPassword && (
                        <small style={{ color: '#27ae60' }}>Les mots de passe correspondent</small>
                      )}
                    </div>
                  </>
                )}

                <div className="form-group">
                  <label htmlFor="idStatus">Statut *</label>
                  <select
                    id="idStatus"
                    value={newUser.idStatus}
                    onChange={(e) => setNewUser({ ...newUser, idStatus: e.target.value })}
                    style={{ color: '#2c3e50', backgroundColor: 'white' }}
                  >
                    <option value="" style={{ color: '#2c3e50', backgroundColor: 'white' }}>Sélectionnez un statut</option>
                    {statuses.map(status => (
                      <option key={status.id} value={status.id} style={{ color: '#2c3e50', backgroundColor: 'white' }}>
                        {status.libelle}
                      </option>
                    ))}
                  </select>
             
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
