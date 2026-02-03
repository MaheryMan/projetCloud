import React, { useEffect, useState } from 'react';
import './SignalementManagement.css';

function SignalementManagement() {
  const [signalements, setSignalements] = useState([]);
  const [filteredSignalements, setFilteredSignalements] = useState([]);
  const [filterStatus, setFilterStatus] = useState('all');
  const [filterType, setFilterType] = useState('tous');
  const [filterEntreprise, setFilterEntreprise] = useState('tous');
  const [searchTerm, setSearchTerm] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [editForm, setEditForm] = useState({});
  const [loading, setLoading] = useState(true);
  const [entreprises, setEntreprises] = useState([]);
  const [photoPreview, setPhotoPreview] = useState(null);
  const [photoFile, setPhotoFile] = useState(null);
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [typesSignalement, setTypesSignalement] = useState([]);


  useEffect(() => {
    fetchEntreprises();
    fetchTypesSignalement();
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

  const fetchTypesSignalement = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('http://localhost:8080/api/types-signalement', {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      });
      if (!response.ok) throw new Error('Erreur de chargement des types');
      const data = await response.json();
      setTypesSignalement(data);
    } catch (error) {
      console.error('Erreur:', error);
    }
  };

  const handlePhotoChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Vérifier la taille (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        alert('La photo ne doit pas dépasser 5MB');
        return;
      }

      // Vérifier le type
      if (!file.type.startsWith('image/')) {
        alert('Le fichier doit être une image');
        return;
      }

      setPhotoFile(file);

      // Créer une préview
      const reader = new FileReader();
      reader.onload = (event) => {
        setPhotoPreview(event.target.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const uploadPhotoToServer = async (file) => {
    try {
      setUploadingPhoto(true);
      const token = localStorage.getItem('token');
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch('http://localhost:8080/api/upload/photo', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      });

      if (!response.ok) {
        throw new Error('Erreur lors de l\'upload');
      }

      const data = await response.json();
      return data.photoUrl || data.url; // Adapter selon la réponse du backend
    } catch (error) {
      console.error('Erreur upload:', error);
      alert('Erreur lors du téléchargement de la photo');
      return null;
    } finally {
      setUploadingPhoto(false);
    }
  };


  useEffect(() => {
    fetchSignalements();
  }, []);

  useEffect(() => {
    filterSignalements();
  }, [signalements, filterStatus, filterType, filterEntreprise, searchTerm]);

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

    // Filtre par type
    if (filterType !== 'tous') {
      filtered = filtered.filter(s => s.typeSignalement?.id === parseInt(filterType));
    }

    // Filtre par entreprise
    if (filterEntreprise !== 'tous') {
      if (filterEntreprise === 'non_attribuee') {
        filtered = filtered.filter(s => !s.idEntreprise);
      } else {
        filtered = filtered.filter(s => s.idEntreprise === parseInt(filterEntreprise));
      }
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
    setPhotoFile(null);
    setPhotoPreview(null);
  };

  const handleSave = async (id) => {
    try {
      const token = localStorage.getItem('token');

      // Upload la photo si une nouvelle a été sélectionnée
      let photoUrl = editForm.photoUrl;
      if (photoFile) {
        photoUrl = await uploadPhotoToServer(photoFile);
        if (!photoUrl) {
          throw new Error('Impossible d\'uploader la photo');
        }
      }

      // On ne garde que les champs attendus par le backend
      const payload = {
        latitude: editForm.latitude,
        longitude: editForm.longitude,
        surfaceM2: editForm.surfaceM2,
        budget: editForm.budget,
        description: editForm.description,
        photoUrl: photoUrl,
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
      setPhotoFile(null);
      setPhotoPreview(null);
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

        <div className="additional-filters">
          <select 
            value={filterStatus} 
            onChange={(e) => setFilterStatus(e.target.value)}
            className="filter-select"
          >
            <option value="all">Tous les statuts</option>
            <option value="nouveau">Nouveaux ({signalements.filter(s => s.idStatus === 4).length})</option>
            <option value="en_cours">En cours ({signalements.filter(s => s.idStatus === 5).length})</option>
            <option value="termine">Terminés ({signalements.filter(s => s.idStatus === 6).length})</option>
          </select>

          <select 
            value={filterType} 
            onChange={(e) => setFilterType(e.target.value)}
            className="filter-select"
          >
            <option value="tous">Tous les types</option>
            {typesSignalement.map((type) => (
              <option key={type.id} value={type.id}>
                {type.libelle}
              </option>
            ))}
          </select>

          <select 
            value={filterEntreprise} 
            onChange={(e) => setFilterEntreprise(e.target.value)}
            className="filter-select"
          >
            <option value="tous">Toutes les entreprises</option>
            <option value="non_attribuee">Non attribuée</option>
            {entreprises.map((entreprise) => (
              <option key={entreprise.id} value={entreprise.id}>
                {entreprise.nom}
              </option>
            ))}
          </select>

          {(filterStatus !== 'all' || filterType !== 'tous' || filterEntreprise !== 'tous') && (
            <button 
              className="filter-reset-btn"
              onClick={() => {
                setFilterStatus('all');
                setFilterType('tous');
                setFilterEntreprise('tous');
              }}
            >
              Réinitialiser
            </button>
          )}
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
             
              <th>Date</th>
              <th>Statut</th>
              <th>Photo</th>
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
                  
                    <td>{formatDate(signal.lastHistoriqueDate || signal.createdAt)}</td>
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
                    <td className="photo-cell">
                      <div className="photo-upload-section">
                        {photoPreview ? (
                          <div className="photo-preview">
                            <img src={photoPreview} alt="Aperçu" />
                            <button
                              type="button"
                              className="btn-remove-photo"
                              onClick={() => {
                                setPhotoFile(null);
                                setPhotoPreview(null);
                              }}
                              title="Supprimer la photo"
                            >
                              ✕
                            </button>
                          </div>
                        ) : editForm.photoUrl ? (
                          <div className="photo-preview">
                            <img src={editForm.photoUrl} alt="Signalement" />
                            <button
                              type="button"
                              className="btn-remove-photo"
                              onClick={() => setEditForm({ ...editForm, photoUrl: null })}
                              title="Supprimer la photo"
                            >
                              ✕
                            </button>
                          </div>
                        ) : (
                          <label className="photo-upload-label">
                            <input
                              type="file"
                              accept="image/*"
                              onChange={handlePhotoChange}
                              disabled={uploadingPhoto}
                              style={{ display: 'none' }}
                            />
                            <span className="upload-icon">📷</span>
                            <span className="upload-text">
                              {uploadingPhoto ? 'Upload...' : 'Ajouter photo'}
                            </span>
                          </label>
                        )}
                      </div>
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
                 
                  <td>
                    {signal.lastHistoriqueDate
                      ? formatDate(signal.lastHistoriqueDate)
                      : signal.createdAt
                      ? formatDate(signal.createdAt)
                      : 'N/A'}
                  </td>
                    <td>
                      <span className={getStatusClass(signal.idStatus)}>
                        {getStatusLabel(signal.idStatus)}
                      </span>
                    </td>
                    <td className="photo-cell">
                      {signal.photoUrl ? (
                        <a href={signal.photoUrl} target="_blank" rel="noopener noreferrer" className="photo-link">
                          📷 Voir photo
                        </a>
                      ) : (
                        <span className="no-photo">Aucune photo</span>
                      )}
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
                          Éditer
                        </button>
                        <button className="btn-delete" onClick={() => handleDelete(signal.id)}>
                          Supprimer
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
