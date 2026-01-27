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
        headers: { 'Authorization': `Bearer ${token}` }
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
      filtered = filtered.filter(s => s.status === filterStatus);
    }

    if (searchTerm) {
      filtered = filtered.filter(s =>
        s.id.toString().includes(searchTerm) ||
        (s.entreprise && s.entreprise.toLowerCase().includes(searchTerm.toLowerCase()))
      );
    }

    setFilteredSignalements(filtered);
  };

  const handleEdit = (signal) => {
    setEditingId(signal.id);
    setEditForm({ ...signal });
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setEditForm({});
  };

  const handleSave = async (id) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/api/signalements/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(editForm)
      });

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

  const getStatusLabel = (status) => {
    const labels = {
      'nouveau': 'Nouveau',
      'en_cours': 'En cours',
      'termine': 'Terminé'
    };
    return labels[status] || status;
  };

  const getStatusClass = (status) => {
    return `status-badge status-${status}`;
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
             Nouveaux ({signalements.filter(s => s.status === 'nouveau').length})
          </button>
          <button
            className={`filter-tab ${filterStatus === 'en_cours' ? 'active' : ''}`}
            onClick={() => setFilterStatus('en_cours')}
          >
             En cours ({signalements.filter(s => s.status === 'en_cours').length})
          </button>
          <button
            className={`filter-tab ${filterStatus === 'termine' ? 'active' : ''}`}
            onClick={() => setFilterStatus('termine')}
          >
             Terminés ({signalements.filter(s => s.status === 'termine').length})
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
                        value={editForm.status}
                        onChange={(e) => setEditForm({ ...editForm, status: e.target.value })}
                        className="edit-select"
                      >
                        <option value="nouveau">Nouveau</option>
                        <option value="en_cours">En cours</option>
                        <option value="termine">Terminé</option>
                      </select>
                    </td>
                    <td>
                      <input
                        type="number"
                        value={editForm.surface}
                        onChange={(e) => setEditForm({ ...editForm, surface: parseFloat(e.target.value) })}
                        className="edit-input"
                      />
                    </td>
                    <td>
                      <input
                        type="number"
                        value={editForm.budget}
                        onChange={(e) => setEditForm({ ...editForm, budget: parseFloat(e.target.value) })}
                        className="edit-input"
                      />
                    </td>
                    <td>
                      <input
                        type="text"
                        value={editForm.entreprise || ''}
                        onChange={(e) => setEditForm({ ...editForm, entreprise: e.target.value })}
                        className="edit-input"
                        placeholder="Nom entreprise"
                      />
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
                    <td>{formatDate(signal.date)}</td>
                    <td>
                      <span className={getStatusClass(signal.status)}>
                        {getStatusLabel(signal.status)}
                      </span>
                    </td>
                    <td>{signal.surface} m²</td>
                    <td>{formatCurrency(signal.budget)}</td>
                    <td>{signal.entreprise || 'Non attribuée'}</td>
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
