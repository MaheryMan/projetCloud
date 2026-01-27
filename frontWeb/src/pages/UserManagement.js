import React, { useEffect, useState } from 'react';
import './UserManagement.css';

function UserManagement() {
  const [users, setUsers] = useState([]);
  const [blockedUsers, setBlockedUsers] = useState([]);
  const [filterType, setFilterType] = useState('blocked');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const token = localStorage.getItem('token');
      
      const [allUsersRes, blockedUsersRes] = await Promise.all([
        fetch('http://localhost:8080/api/users', {
          headers: { 'Authorization': `Bearer ${token}` }
        }),
        fetch('http://localhost:8080/api/users/blocked', {
          headers: { 'Authorization': `Bearer ${token}` }
        })
      ]);

      const allUsers = await allUsersRes.json();
      const blocked = await blockedUsersRes.json();

      setUsers(allUsers);
      setBlockedUsers(blocked);
    } catch (error) {
      console.error('Erreur:', error);
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
        headers: { 'Authorization': `Bearer ${token}` }
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

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
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

  if (loading) {
    return <div className="loading">Chargement...</div>;
  }

  return (
    <div className="user-management">
      <header className="page-header">
        <h1>Gestion des Utilisateurs</h1>
        <p>Débloquer les comptes et gérer les utilisateurs</p>
      </header>

      <div className="stats-cards">
        <div className="stat-card">
          <div className="stat-icon">👥</div>
          <div className="stat-content">
            <div className="stat-value">{users.length}</div>
            <div className="stat-label">Total utilisateurs</div>
          </div>
        </div>

        <div className="stat-card danger">
          <div className="stat-icon">🔒</div>
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
          🔒 Utilisateurs bloqués ({blockedUsers.length})
        </button>
        <button
          className={`filter-btn ${filterType === 'all' ? 'active' : ''}`}
          onClick={() => setFilterType('all')}
        >
          👥 Tous les utilisateurs ({users.length})
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
                    {user.tentatives >= 3 && (
                      <button
                        className="btn-unblock"
                        onClick={() => handleUnblock(user.id)}
                        title="Débloquer l'utilisateur"
                      >
                        🔓 Débloquer
                      </button>
                    )}
                    <button
                      className="btn-reset"
                      onClick={() => handleResetPassword(user.id)}
                      title="Réinitialiser le mot de passe"
                    >
                      🔑 Reset
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
              ? '🎉 Aucun utilisateur bloqué !' 
              : 'Aucun utilisateur trouvé'}
          </div>
        )}
      </div>

      <div className="info-box">
        <h3>ℹ️ Informations</h3>
        <ul>
          <li>Les utilisateurs sont automatiquement bloqués après <strong>3 tentatives</strong> de connexion échouées</li>
          <li>Utilisez le bouton "Débloquer" pour réinitialiser le compteur de tentatives</li>
          <li>Le bouton "Reset" génère un mot de passe temporaire pour l'utilisateur</li>
          <li>Les utilisateurs peuvent se débloquer après un certain délai (configurable dans les paramètres)</li>
        </ul>
      </div>
    </div>
  );
}

export default UserManagement;
