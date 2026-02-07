import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import './SignalementDetails.css';

function SignalementDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [signalement, setSignalement] = useState(null);
  const [loading, setLoading] = useState(true);
  const [historique, setHistorique] = useState([]);
  const [users, setUsers] = useState([]);
  const [statuses, setStatuses] = useState([]);

  useEffect(() => {
    fetchSignalementDetails();
    fetchHistorique();
    fetchUsers();
    fetchStatuses();
  }, [id]);

  const fetchSignalementDetails = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/api/signalements/${id}`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      });
      if (!response.ok) throw new Error('Erreur de chargement');
      const data = await response.json();
      setSignalement(data);
    } catch (error) {
      console.error('Erreur:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchHistorique = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/api/historiques/signalement/${id}`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      });
      if (response.ok) {
        const data = await response.json();
        setHistorique(data);
      }
    } catch (error) {
      console.error('Erreur historique:', error);
    }
  };

  const fetchUsers = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('http://localhost:8080/api/users', {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      });
      if (response.ok) {
        const data = await response.json();
        setUsers(data);
      }
    } catch (error) {
      console.error('Erreur utilisateurs:', error);
    }
  };

  const fetchStatuses = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('http://localhost:8080/api/status', {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      });
      if (response.ok) {
        const data = await response.json();
        setStatuses(data);
      }
    } catch (error) {
      console.error('Erreur statuts:', error);
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'MGA',
      minimumFractionDigits: 0
    }).format(amount);
  };

  const getStatusLabel = (statusId) => {
    const status = statuses.find(s => s.id === statusId);
    if (status) return status.libelle;
    const map = {
      1: 'Actif',
      2: 'Bloqué',
      3: 'Inactif',
      4: 'Nouveau',
      5: 'En cours',
      6: 'Terminé',
      7: 'Annulé',
      8: 'Créé'
    };
    return map[statusId] || statusId;
  };

  const getUserName = (userId) => {
    const user = users.find(u => u.id === userId);
    return user ? `${user.prenom} ${user.nom}` : 'Utilisateur inconnu';
  };

  const getStatusClass = (statusId) => {
    return `status-badge status-${statusId}`;
  };

  if (loading) {
    return <div className="loading">Chargement...</div>;
  }

  if (!signalement) {
    return <div className="error">Signalement introuvable</div>;
  }

  return (
    <div className="signalement-details">
      <div className="details-header">
        <button className="btn-back" onClick={() => navigate(-1)}>
          ← Retour
        </button>
        <h1>Détails du signalement #{signalement.id}</h1>
      </div>

      <div className="details-content">
        <div className="details-main">
          <div className="info-card">
            <h2>Informations générales</h2>
            <div className="info-grid">
              <div className="info-item">
                <label>Statut</label>
                <span className={getStatusClass(signalement.idStatus)}>
                  {getStatusLabel(signalement.idStatus)}
                </span>
              </div>
              <div className="info-item">
                <label>Date de création</label>
                <span>{formatDate(signalement.createdAt || signalement.lastHistoriqueDate)}</span>
              </div>
              <div className="info-item">
                <label>Type</label>
                <span>{signalement.typeSignalement?.libelle || 'Non spécifié'}</span>
              </div>
              <div className="info-item">
                <label>Surface</label>
                <span>{signalement.surfaceM2} m²</span>
              </div>
              <div className="info-item">
                <label>Budget</label>
                <span className="budget">{formatCurrency(signalement.budget)}</span>
              </div>
              <div className="info-item">
                <label>Entreprise</label>
                <span>{signalement.entreprise?.nom || 'Non attribuée'}</span>
              </div>
            </div>
          </div>

          <div className="info-card">
            <h2>Localisation</h2>
            <div className="location-info">
              <div className="coordinates">
                <span className="coord-label">Latitude :</span>
                <span className="coord-value">{signalement.latitude?.toFixed(6)}</span>
              </div>
              <div className="coordinates">
                <span className="coord-label">Longitude :</span>
                <span className="coord-value">{signalement.longitude?.toFixed(6)}</span>
              </div>
            </div>
          </div>

          {signalement.description && (
            <div className="info-card">
              <h2>Description</h2>
              <p className="description">{signalement.description}</p>
            </div>
          )}

          {historique.length > 0 && (
            <div className="info-card">
              <h2>Historique des changements de statut</h2>
              <div className="historique-list">
                {historique.map((item, index) => (
                  <div key={item.id || index} className="historique-item">
                    <div className="historique-header">
                      <div className="historique-date">{formatDate(item.createdAt)}</div>
                      <span className={getStatusClass(item.idStatus)}>
                        {getStatusLabel(item.idStatus)}
                      </span>
                    </div>
                    <div className="historique-body">
                      <div className="historique-user">
                        <strong>Par:</strong> {getUserName(item.idUtilisateur)}
                      </div>
                      {item.commentaire && (
                        <div className="historique-comment">
                          <strong>Commentaire:</strong> {item.commentaire}
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="details-sidebar">
        </div>
      </div>

      {signalement.photos && signalement.photos.length > 0 && (
        <div className="info-card photos-card">
          <h2>Photos ({signalement.photos.length})</h2>
          <div className="photos-gallery">
            {signalement.photos.map((photo, index) => (
              <a 
                key={photo.id || index} 
                href={photo.url} 
                target="_blank" 
                rel="noopener noreferrer"
                className="photo-item"
              >
                <img src={photo.url} alt={`Photo ${index + 1}`} />
              </a>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default SignalementDetails;
