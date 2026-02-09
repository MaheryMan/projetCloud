import React, { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaCamera, FaSync, FaUpload, FaDownload, FaTerminal, FaTimes } from 'react-icons/fa';
import './SignalementManagement.css';
import './sync-terminal.css';

function SignalementManagement() {
  const navigate = useNavigate();
  const [signalements, setSignalements] = useState([]);
  const [filteredSignalements, setFilteredSignalements] = useState([]);
  const [filterStatus, setFilterStatus] = useState('all');
  const [filterType, setFilterType] = useState('tous');
  const [filterEntreprise, setFilterEntreprise] = useState('tous');
  const [searchTerm, setSearchTerm] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [editForm, setEditForm] = useState({});
  const [showEditModal, setShowEditModal] = useState(false);
  const [loading, setLoading] = useState(true);
  const [entreprises, setEntreprises] = useState([]);
  const [photoPreview, setPhotoPreview] = useState(null);
  const [photoFile, setPhotoFile] = useState(null);
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [typesSignalement, setTypesSignalement] = useState([]);
  const [syncLoading, setSyncLoading] = useState(false);
  const [syncLog, setSyncLog] = useState([]);
  const terminalBodyRef = useRef(null);

  // Auto-scroll vers le bas du terminal
  useEffect(() => {
    if (terminalBodyRef.current) {
      terminalBodyRef.current.scrollTop = terminalBodyRef.current.scrollHeight;
    }
  }, [syncLog, syncLoading]);

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
        if (filterStatus === 'cree') return s.idStatus === 8;
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
    // S'assurer que idStatus est toujours un nombre
    const statusId = Number(signal.idStatus ?? signal.status ?? 4);
    setEditForm({
      ...signal,
      surfaceM2: signal.surfaceM2 ?? signal.surface ?? '',
      idEntreprise: signal.idEntreprise ?? signal.entrepriseId ?? '',
      idStatus: statusId,
    });
    setShowEditModal(true);
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setEditForm({});
    setPhotoFile(null);
    setPhotoPreview(null);
    setShowEditModal(false);
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
      setShowEditModal(false);
      alert('Signalement mis à jour avec succès');
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
    // Mapping des IDs vers les libellés
    const idMap = {
      1: 'Actif',
      2: 'Bloqué',
      3: 'Inactif',
      4: 'Nouveau',
      5: 'En cours',
      6: 'Terminé',
      7: 'Annulé',
      8: 'Créé',
    };

    // Si c'est un objet status avec libelle
    if (typeof statusOrId === 'object' && statusOrId !== null && statusOrId.libelle) {
      return statusOrId.libelle;
    }

    // Si c'est un nombre (ID)
    if (typeof statusOrId === 'number') {
      return idMap[statusOrId] || statusOrId;
    }

    // Si c'est une string (en_cours, termine, etc)
    const stringMap = {
      'nouveau': 'Nouveau',
      'en_cours': 'En cours',
      'termine': 'Terminé',
      'annule': 'Annulé',
      'actif': 'Actif',
      'bloque': 'Bloqué',
      'inactif': 'Inactif',
      'cree': 'Créé',
    };

    const normalized = String(statusOrId).toLowerCase().replace(/[\s_-]/g, '_');
    return stringMap[normalized] || statusOrId;
  };

  const getStatusClass = (statusOrId) => {
    // Extraire l'ID numérique s'il y a un objet status
    let statusId = statusOrId;
    if (typeof statusOrId === 'object' && statusOrId !== null && statusOrId.id) {
      statusId = statusOrId.id;
    }
    return `status-badge status-${statusId}`;
  };

  const getEntrepriseName = (idEntreprise) => {
    const ent = entreprises.find(e => e.id === idEntreprise);
    return ent ? ent.nom : 'Non attribuée';
  };

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

  const handleSyncFirebaseToPostgres = async () => {
    if (!window.confirm('Êtes-vous sûr de vouloir synchroniser les signalements de Firebase vers PostgreSQL ?')) {
      return;
    }

    try {
      setSyncLoading(true);
      addSessionSeparator('FIREBASE → POSTGRESQL');
      addLog('Démarrage de la synchronisation...', 'info');
      
      const token = localStorage.getItem('token');
      
      const response = await fetch('http://localhost:8080/api/sync/firebase-to-postgres', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        }
      });

      const data = await response.json();

      if (!response.ok) {
        // Afficher le message d'erreur du serveur
        const errorMessage = data.message || 'Erreur lors de la synchronisation';
        addLog(errorMessage, 'error', { icon: '❌' });
        // Afficher uniquement les erreurs supplémentaires qui ne sont pas identiques au message principal
        if (data.errors && data.errors.length > 0) {
          data.errors.forEach(err => {
            if (err !== errorMessage) {
              addLog(err, 'error');
            }
          });
        }
        return;
      }

      addLog('Synchronisation réussie!', 'success', { 
        badge: `${data.successCount} signalements`,
        icon: '✅' 
      });

      // Recharger la liste des signalements
      await fetchSignalements();
      addLog('Liste des signalements rechargée', 'success');
    } catch (error) {
      console.error('Erreur:', error);
      addLog(`Erreur: ${error.message}`, 'error', { icon: '❌' });
    } finally {
      setSyncLoading(false);
    }
  };

  const handleSyncPostgresToFirebase = async () => {
    if (!window.confirm('Êtes-vous sûr de vouloir synchroniser les signalements de PostgreSQL vers Firebase ?')) {
      return;
    }

    try {
      setSyncLoading(true);
      addSessionSeparator('POSTGRESQL → FIREBASE');
      addLog('Démarrage de la synchronisation...', 'info');
      const token = localStorage.getItem('token');
      
      const response = await fetch('http://localhost:8080/api/sync/postgres-to-firebase', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        }
      });

      const data = await response.json();

      if (!response.ok) {
        // Afficher le message d'erreur du serveur
        const errorMessage = data.message || 'Erreur lors de la synchronisation';
        addLog(errorMessage, 'error', { icon: '❌' });
        // Afficher uniquement les erreurs supplémentaires qui ne sont pas identiques au message principal
        if (data.errors && data.errors.length > 0) {
          data.errors.forEach(err => {
            if (err !== errorMessage) {
              addLog(err, 'error');
            }
          });
        }
        return;
      }

      addLog('Synchronisation réussie!', 'success', { 
        badge: `${data.successCount} signalements`,
        icon: '✅'
      });

      // Recharger la liste des signalements
      await fetchSignalements();
      addLog('Liste des signalements rechargée', 'success');
    } catch (error) {
      console.error('Erreur:', error);
      addLog(`Erreur: ${error.message}`, 'error', { icon: '❌' });
    } finally {
      setSyncLoading(false);
    }
  };

  return (
    <div className="signalement-management">
      <header className="page-header">
        <h1>Gestion des Signalements</h1>
        <p>Gérer les informations et statuts des signalements</p>
        {loading && (
          <div className="inline-loading">
            <div className="loading-spinner"></div>
            <span>Chargement initial...</span>
          </div>
        )}
      </header>

      {/* Section de synchronisation */}
      <div className="sync-section">
        <div className="sync-header">
          <h3><FaSync /> Synchronisation Firebase</h3>
          <p>Gérer la synchronisation bidirectionnelle des signalements</p>
        </div>
        
        <div className="sync-content">
          <div className="sync-buttons">
            <button 
              className="btn-sync primary" 
              onClick={handleSyncFirebaseToPostgres} 
              disabled={syncLoading}
              title="Firebase → PostgreSQL"
            >
              <FaDownload /> Depuis Firebase
            </button>
            <button 
              className="btn-sync secondary" 
              onClick={handleSyncPostgresToFirebase} 
              disabled={syncLoading}
              title="PostgreSQL → Firebase"
            >
              <FaUpload /> Vers Firebase
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
              {syncLoading && (
                <div className="sync-terminal-line sync-loading">
                  <span className="sync-icon">⟳</span>
                  <span className="sync-message-text">En cours...</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

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
            <option value="cree">Créés ({signalements.filter(s => s.idStatus === 8).length})</option>
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
              <tr 
                key={signal.id}
                className='clickable-row'
                onClick={() => navigate(`/signalements/${signal.id}`)}
              >
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
                  {signal.photos && signal.photos.length > 0 ? (
                    <div className="photo-link-wrapper">
                      <span className="photo-link">
                        <FaCamera /> {signal.photos.length} photo{signal.photos.length > 1 ? 's' : ''}
                      </span>
                      <div className="photo-preview-tooltip">
                        <div className="photo-grid">
                          {signal.photos.map((photo, index) => (
                            <a 
                              key={photo.id || index} 
                              href={photo.url} 
                              target="_blank" 
                              rel="noopener noreferrer"
                              className="photo-thumbnail"
                            >
                              <img src={photo.url} alt={`Photo ${index + 1}`} />
                            </a>
                          ))}
                        </div>
                      </div>
                    </div>
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
                  <div className="action-buttons" onClick={(e) => e.stopPropagation()}>
                    <button className="btn-edit" onClick={() => handleEdit(signal)}>
                      Éditer
                    </button>
                    <button className="btn-delete" onClick={() => handleDelete(signal.id)}>
                      Supprimer
                    </button>
                  </div>
                </td>
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

      {/* Modal d'édition de signalement */}
      {showEditModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h2>Modifier le signalement</h2>
              <button 
                className="btn-close-modal" 
                onClick={handleCancelEdit}
              >
                ✕
              </button>
            </div>

            <div className="modal-body">
              <div className="user-form">
                <div className="form-group">
                  <label htmlFor="statut">Statut *</label>
                  <select
                    id="statut"
                    value={String(editForm.idStatus || '4')}
                    onChange={(e) => setEditForm({ ...editForm, idStatus: parseInt(e.target.value) })}
                    style={{ color: '#2c3e50', backgroundColor: 'white' }}
                  >
                    <option value="8">Créé</option>
                    <option value="4">Nouveau</option>
                    <option value="5">En cours</option>
                    <option value="6">Terminé</option>
                  </select>
                </div>

                <div className="form-group">
                  <label htmlFor="photo">Photo</label>
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
                        <span className="upload-icon"><FaCamera /></span>
                        <span className="upload-text">
                          {uploadingPhoto ? 'Upload...' : 'Ajouter photo'}
                        </span>
                      </label>
                    )}
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="surface">Surface (m²) *</label>
                  <input
                    type="number"
                    id="surface"
                    value={editForm.surfaceM2 ?? 0}
                    onChange={(e) => setEditForm({ ...editForm, surfaceM2: parseFloat(e.target.value) })}
                    placeholder="Entrez la surface"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="budget">Budget *</label>
                  <input
                    type="number"
                    id="budget"
                    value={editForm.budget ?? 0}
                    onChange={(e) => setEditForm({ ...editForm, budget: parseFloat(e.target.value) })}
                    placeholder="Entrez le budget"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="entreprise">Entreprise</label>
                  <select
                    id="entreprise"
                    value={editForm.idEntreprise ?? ''}
                    onChange={(e) => setEditForm({ ...editForm, idEntreprise: parseInt(e.target.value) })}
                    style={{ color: '#2c3e50', backgroundColor: 'white' }}
                  >
                    <option value="">Sélectionner une entreprise</option>
                    {entreprises.map((ent) => (
                      <option key={ent.id} value={ent.id}>
                        {ent.nom}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label htmlFor="description">Description</label>
                  <textarea
                    id="description"
                    value={editForm.description || ''}
                    onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                    placeholder="Entrez une description"
                    rows="4"
                  />
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button 
                className="btn-save-config" 
                onClick={() => handleSave(editingId)}
              >
                Enregistrer
              </button>
              <button 
                className="btn-close-modal" 
                onClick={handleCancelEdit}
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

export default SignalementManagement;
