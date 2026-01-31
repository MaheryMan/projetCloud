import React, { useEffect, useState } from 'react';
import './SignalementManagement.css';

function SignalementManagement() {
  const [signalements, setSignalements] = useState([]);
  const [filteredSignalements, setFilteredSignalements] = useState([]);
  const [filterStatus, setFilterStatus] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [editForm, setEditForm] = useState({});
  const [loading, setLoading] = useState(true);
  const[entreprises, setEntreprises]=useState([]);


  useEffect(() => {
    fetchEntreprises();
  }, []);

const fetchEntreprises = async () => {
  try {
    const token = localStorage.getItem('token');
    const response = await fetch('http://localhost:8080/api/entreprises', {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Erreur de chargement des entreprises');
    const data = await response.json();
    setEntreprises(data);
  } catch (error) {
    console.error('Erreur:', error);
  }
};


  useEffect(() => {
    fetchSignalements();
  }, []);

  useEffect(() => {
    filterSignalements();
  }, [signalements, filterStatus, searchTerm]);

  const fetchSignalements = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('http://localhost:8080/api/signalements', {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      });
      if (!response.ok) throw new Error('Erreur de chargement');
      const data = await response.json();
      setSignalements(data);
    } catch (error) {
      console.error('Erreur:', error);
    } finally {
      setLoading(false);
    }
  };

  const filterSignalements = () => {
    let filtered = [...signalements];

    if (filterStatus !== 'all') {
      filtered = filtered.filter(s => {
        if (filterStatus === 'nouveau') return s.idStatus === 4;
        if (filterStatus === 'en_cours') return s.idStatus === 5;
        if (filterStatus === 'termine') return s.idStatus === 6;
        return true;
      });
    }

    if (searchTerm) {
      filtered = filtered.filter(s =>
        s.id.toString().includes(searchTerm) ||
        (getEntrepriseName(s.idEntreprise).toLowerCase().includes(searchTerm.toLowerCase()))
      );
    }

    setFilteredSignalements(filtered);
  };

  const handleEdit = (signal) => {
    setEditingId(signal.id);
    setEditForm({
      ...signal,
      surfaceM2: signal.surfaceM2 ?? signal.surface ?? '',
      idEntreprise: signal.idEntreprise ?? signal.entrepriseId ?? '',
      idStatus: signal.idStatus ?? signal.status ?? '',
    });
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setEditForm({});
  };

  const handleSave = async (id) => {
    try {
      const token = localStorage.getItem('token');
      // On ne garde que les champs attendus par le backend
      const payload = {
        latitude: editForm.latitude,
        longitude: editForm.longitude,
        surfaceM2: editForm.surfaceM2,
        budget: editForm.budget,
        description: editForm.description,
        photoUrl: editForm.photoUrl,
        idTypeSignalement: editForm.idTypeSignalement,
        idStatus: editForm.idStatus,
        idEntreprise: editForm.idEntreprise,
        idUtilisateur: editForm.idUtilisateur,
      };
      const response = await fetch(`http://localhost:8080/api/signalements/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });
      
      const result = await response.json();
      console.log('Réponse du backend:', result);

      if (!response.ok) throw new Error('Erreur de mise à jour');

      await fetchSignalements();
      setEditingId(null);
      setEditForm({});
    } catch (error) {
      console.error('Erreur:', error);
      alert('Erreur lors de la mise à jour');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Êtes-vous sûr de vouloir supprimer ce signalement ?')) {
      return;
    }

    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/api/signalements/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!response.ok) throw new Error('Erreur de suppression');

      await fetchSignalements();
    } catch (error) {
      console.error('Erreur:', error);
      alert('Erreur lors de la suppression');
    }
  };

  const handleChangeStatus = async (signal, newStatus) => {
    try {
      const token = localStorage.getItem('token');
      const payload = {
        ...signal,
        idStatus: newStatus
      };
      const response = await fetch(`http://localhost:8080/api/signalements/${signal.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify(payload)
      });
      if (!response.ok) throw new Error('Erreur lors du changement de statut');
      await fetchSignalements();
    } catch (error) {
      alert('Erreur lors du changement de statut');
      console.error(error);
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('fr-FR');
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'MGA',
      minimumFractionDigits: 0
    }).format(amount);
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
    const ent = entreprises.find(e => e.id === idEntreprise);
    return ent ? ent.nom : 'Non attribuée';
  };

  if (loading) {
    return <div className="loading">Chargement...</div>;
  }

  return (
    <div className="signalement-management">
      <header className="page-header">
        <h1>Gestion des Signalements</h1>
        <p>Gérer les informations et statuts des signalements</p>
      </header>

      <div className="filters-section">
        <div className="search-box">
          <input
            type="text"
            placeholder=" Rechercher par ID ou entreprise..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>

        <div className="filter-tabs">
          <button
            className={`filter-tab ${filterStatus === 'all' ? 'active' : ''}`}
            onClick={() => setFilterStatus('all')}
          >
            Tous ({signalements.length})
          </button>
          <button
            className={`filter-tab ${filterStatus === 'nouveau' ? 'active' : ''}`}
            onClick={() => setFilterStatus('nouveau')}
          >
             Nouveaux ({signalements.filter(s => s.idStatus === 4).length})
          </button>
          <button
            className={`filter-tab ${filterStatus === 'en_cours' ? 'active' : ''}`}
            onClick={() => setFilterStatus('en_cours')}
          >
             En cours ({signalements.filter(s => s.idStatus === 5).length})
          </button>
          <button
            className={`filter-tab ${filterStatus === 'termine' ? 'active' : ''}`}
            onClick={() => setFilterStatus('termine')}
          >
             Terminés ({signalements.filter(s => s.idStatus === 6).length})
          </button>
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Date</th>
              <th>Statut</th>
              <th>Surface (m²)</th>
              <th>Budget</th>
              <th>Entreprise</th>
              <th>Localisation</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredSignalements.map((signal) => (
              <tr key={signal.id}>
                {editingId === signal.id ? (
                  <>
                    <td>#{signal.id}</td>
                    <td>{formatDate(signal.date)}</td>
                    <td>
                      <select
                        value={editForm.idStatus}
                        onChange={(e) => setEditForm({ ...editForm, idStatus: parseInt(e.target.value) })}
                        className="edit-select"
                        style={{ minWidth: 110 }}
                      >
                        <option value={4}>Nouveau</option>
                        <option value={5}>En cours</option>
                        <option value={6}>Terminé</option>
                      </select>
                    </td>
                    <td>
                      <input
                        type="number"
                        value={editForm.surfaceM2 ?? 0}
                        onChange={(e) => setEditForm({ ...editForm, surfaceM2: parseFloat(e.target.value) })}
                        className="edit-input"
                      />
                    </td>
                    <td>
                      <input
                        type="number"
                        value={editForm.budget ?? 0}
                        onChange={(e) => setEditForm({ ...editForm, budget: parseFloat(e.target.value) })}
                        className="edit-input"
                      />
                    </td>
                    <td>
                      <select
                        value={editForm.idEntreprise ?? ''}
                        onChange={(e) => setEditForm({ ...editForm, idEntreprise: parseInt(e.target.value) })}
                        className="edit-select"
                      >
                        <option value="">Sélectionner une entreprise</option>
                        {entreprises.map((ent) => (
                          <option key={ent.id} value={ent.id}>
                            {ent.nom}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td className="location-cell">
                      {signal.latitude.toFixed(4)}, {signal.longitude.toFixed(4)}
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button className="btn-save" onClick={() => handleSave(signal.id)}>
                          ✓
                        </button>
                        <button className="btn-cancel" onClick={handleCancelEdit}>
                          ✕
                        </button>
                      </div>
                    </td>
                  </>
                ) : (
                  <>
                    <td>#{signal.id}</td>
                  <td>
                    {signal.typeSignalement?.createdAt
                      ? formatDate(signal.typeSignalement.createdAt)
                      : ''}
                  </td>
                    <td>
                      <span className={getStatusClass(signal.idStatus)}>
                        {getStatusLabel(signal.idStatus)}
                      </span>
                    </td>
                    <td>{signal.surfaceM2} m²</td>
                    <td>{formatCurrency(signal.budget)}</td>
                    <td>{getEntrepriseName(signal.idEntreprise)}</td>
                    <td className="location-cell">
                       {signal.latitude.toFixed(4)}, {signal.longitude.toFixed(4)}
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button className="btn-edit" onClick={() => handleEdit(signal)}>
                          
                        </button>
                        <button className="btn-delete" onClick={() => handleDelete(signal.id)}>
                          
                        </button>
                      </div>
                    </td>
                  </>
                )}
              </tr>
            ))}
          </tbody>
        </table>

        {filteredSignalements.length === 0 && (
          <div className="no-results">
            Aucun signalement trouvé
          </div>
        )}
      </div>
    </div>
  );
}

export default SignalementManagement;
