import React, { useEffect, useState, useRef } from 'react';
import { auth } from '../firebase';
import { useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import './VisitorMap.css';

// Fix pour les icônes Leaflet
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
  iconUrl: require('leaflet/dist/images/marker-icon.png'),
  shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});


const getMarkerIcon = (status) => {
  const colors = {
    'nouveau': '#e74c3c',
    'en_cours': '#f39c12',
    'termine': '#27ae60'
  };

  return new L.Icon({
    iconUrl: `data:image/svg+xml;base64,${btoa(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="${colors[status] || '#3498db'}" width="32" height="32">
        <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
      </svg>
    `)}`,
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32]
  });
};

function VisitorMap() {
  const navigate = useNavigate();
  const [signalements, setSignalements] = useState([]);
  const [stats, setStats] = useState({
    total: 0,
    surfaceTotal: 0,
    budgetTotal: 0,
    avancement: 0
  });
  const [displayStats, setDisplayStats] = useState({
    total: 0,
    surfaceTotal: 0,
    budgetTotal: 0,
    avancement: 0
  });
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [newSignalement, setNewSignalement] = useState({
    latitude: 0,
    longitude: 0,
    description: '',
    surface: 0,
    idTypeSignalement: '',
    photos: [] // Array de fichiers photos
  });
  const [photoPreviewUrls, setPhotoPreviewUrls] = useState([]);
  const [isAddingMode, setIsAddingMode] = useState(false);
  const [typesSignalement, setTypesSignalement] = useState([]);
  const [entreprises, setEntreprises] = useState([]);
  const [user, setUser] = useState(null);
  const [filteredSignalements, setFilteredSignalements] = useState([]);

  // États pour les filtres
  const [filterStatus, setFilterStatus] = useState('tous');
  const [filterType, setFilterType] = useState('tous');
  const [filterEntreprise, setFilterEntreprise] = useState('tous');
  const [filterUser, setFilterUser] = useState(false);

  const position = [-18.909855, 47.525637];

  const getAuthToken = async () => {
    // Prend le token Firebase si connecté, sinon fallback sur le token backend
    if (auth.currentUser) {
      return await auth.currentUser.getIdToken();
    }
    return localStorage.getItem('token');
  };

  const fetchEntreprises = async () => {
    try {
      console.log('fetchEntreprises: début du chargement...');
      const token = localStorage.getItem('token');
      const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
      console.log('fetchEntreprises: headers =', headers);
      const response = await fetch('http://localhost:8080/api/entreprises', { headers });
      console.log('fetchEntreprises: response status =', response.status);
      if (!response.ok) throw new Error('Erreur de chargement des entreprises');
      const data = await response.json();
      console.log('fetchEntreprises: données reçues =', data);
      setEntreprises(data);
    } catch (error) {
      console.error('fetchEntreprises: ERREUR =', error);
    }
  };

  const handleLogout = () => {
    setUser(null);
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    window.location.href = '/login';
  };

  useEffect(() => {
    // Charger l'utilisateur connecté
    const storedUser = localStorage.getItem('user');
    if (storedUser && storedUser !== 'undefined') {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        console.error('Error parsing user:', e);
      }
    }
    
    fetchSignalements();
    fetchTypesSignalement();
    fetchEntreprises();
  }, []);

  // Appliquer les filtres aux signalements
  useEffect(() => {
    let filtered = [...signalements];

    // Filtre par statut
    if (filterStatus !== 'tous') {
      const statusMap = {
        'nouveau': 4,
        'en_cours': 5,
        'termine': 6
      };
      filtered = filtered.filter(s => s.idStatus === statusMap[filterStatus]);
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

    // Filtre "Mes signalements"
    if (filterUser && user) {
      filtered = filtered.filter(s => s.utilisateur?.id === user.id);
    }

    setFilteredSignalements(filtered);
  }, [signalements, filterStatus, filterType, filterEntreprise, filterUser, user]);

  // Calculer les statistiques dynamiquement depuis les signalements filtrés
  useEffect(() => {
    const total = filteredSignalements.length;
    const surfaceTotal = filteredSignalements.reduce((sum, s) => sum + (s.surfaceM2 || 0), 0);
    const budgetTotal = filteredSignalements.reduce((sum, s) => sum + (s.budget || 0), 0);
    const termines = filteredSignalements.filter(s => s.idStatus === 6).length;
    const avancement = total > 0 ? Math.round((termines / total) * 100) : 0;

    setDisplayStats({
      total,
      surfaceTotal,
      budgetTotal,
      avancement
    });
  }, [filteredSignalements]);

  const fetchTypesSignalement = async () => {
    try {
      const token = await getAuthToken();
      const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
      const response = await fetch('http://localhost:8080/api/types-signalement', { headers });
      if (!response.ok) throw new Error('Erreur de chargement des types');
      const data = await response.json();
      setTypesSignalement(data);
    } catch (error) {
      console.error('Erreur lors du chargement des types:', error);
    }
  };

  const fetchSignalements = async () => {
    try {
      const token = await getAuthToken();
      const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
      
      // Récupérer les signalements et les stats en parallèle
      const [signalementsRes, statsRes] = await Promise.all([
        fetch('http://localhost:8080/api/signalements', { headers }),
        fetch('http://localhost:8080/api/signalements/stats', { headers })
      ]);
      
      if (!signalementsRes.ok) throw new Error('Erreur de chargement des signalements');
      if (!statsRes.ok) throw new Error('Erreur de chargement des stats');
      
      const signalementsData = await signalementsRes.json();
      const statsData = await statsRes.json();
      
      setSignalements(signalementsData);
      setStatsFromAPI(statsData);
    } catch (error) {
      console.error('Erreur:', error);
    } finally {
      setLoading(false);
    }
  };

  const setStatsFromAPI = (statsData) => {
    // Utiliser les mêmes stats que ManagerDashboard
    const stats = {
      total: parseInt(statsData.totalSignalements) || 0,
      surfaceTotal: parseFloat(statsData.surfaceTotal) || 0,
      budgetTotal: parseFloat(statsData.chiffreAffaire) || 0, // chiffreAffaire correspond au budget total
      avancement: parseInt(statsData.avancement) || 0
    };
    
    setStats(stats);
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

  const handleMapClick = (e) => {
    if (isAddingMode) {
      // Vérifier si l'utilisateur est connecté (token présent)
      getAuthToken().then(token => {
        if (!token) {
          setIsAddingMode(false);
          setShowModal(false);
          alert('Vous devez être connecté pour ajouter un signalement.');
          navigate('/login');
        } else {
          setNewSignalement({
            ...newSignalement,
            latitude: e.latlng.lat,
            longitude: e.latlng.lng
          });
          setShowModal(true);
        }
      });
    }
  };

const handleSubmitSignalement = async () => {
    try {
      // Récupérer l'ID utilisateur depuis localStorage (si connecté)
      const token = await getAuthToken();
      const userId = localStorage.getItem('user');
      
      // Upload des photos vers Firebase Storage ou autre service
      const photoUrls = await uploadPhotos(newSignalement.photos);
      
      // Préparer les headers
      const headers = {
        'Content-Type': 'application/json'
      };
      // Ajouter le token si l'utilisateur est connecté
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const response = await fetch('http://localhost:8080/api/signalements', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({
          latitude: newSignalement.latitude,
          longitude: newSignalement.longitude,
          description: newSignalement.description,
          surfaceM2: newSignalement.surface || 0,
          budget: 0, // Budget par défaut
          idTypeSignalement: parseInt(newSignalement.idTypeSignalement), // Convertir en nombre
          idStatus: 1, // Statut "nouveau" par défaut
          idEntreprise: null,
          photoUrls: photoUrls, // Array d'URLs de photos
          // Utiliser l'ID de l'utilisateur connecté OU l'ID anonyme (1)
          idUtilisateur: userId ? parseInt(userId) : 1
        })
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Erreur ${response.status}: ${errorText}`);
      }

      alert('✅ Signalement ajouté avec succès !');
      setShowModal(false);
      setIsAddingMode(false);
      setNewSignalement({ latitude: 0, longitude: 0, description: '', surface: 0, idTypeSignalement: '', photos: [] });
      setPhotoPreviewUrls([]);
      await fetchSignalements();
    } catch (error) {
      console.error('Erreur:', error);
      alert('❌ Erreur lors de l\'ajout du signalement: ' + error.message);
    }
  };

  // Fonction pour uploader les photos vers le backend
  const uploadPhotos = async (photos) => {
    if (!photos || photos.length === 0) return [];
    
    try {
      const formData = new FormData();
      photos.forEach(photo => {
        formData.append('files', photo);
      });
      
      const response = await fetch('http://localhost:8080/api/photos/upload', {
        method: 'POST',
        body: formData
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Erreur upload: ${errorText}`);
      }
      
      const uploadedUrls = await response.json();
      return uploadedUrls;
    } catch (error) {
      console.error('Erreur lors de l\'upload des photos:', error);
      throw error;
    }
  };

  // Gestion de la sélection de photos
  const handlePhotoChange = (e) => {
    const files = Array.from(e.target.files);
    const maxPhotos = 5; // Limite de 5 photos
    
    if (newSignalement.photos.length + files.length > maxPhotos) {
      alert(`Vous ne pouvez ajouter que ${maxPhotos} photos maximum`);
      return;
    }
    
    // Ajouter les nouvelles photos
    setNewSignalement({
      ...newSignalement,
      photos: [...newSignalement.photos, ...files]
    });
    
    // Créer les URLs de prévisualisation
    const newPreviewUrls = files.map(file => URL.createObjectURL(file));
    setPhotoPreviewUrls([...photoPreviewUrls, ...newPreviewUrls]);
  };

  // Supprimer une photo
  const handleRemovePhoto = (index) => {
    const newPhotos = [...newSignalement.photos];
    newPhotos.splice(index, 1);
    setNewSignalement({
      ...newSignalement,
      photos: newPhotos
    });
    
    const newPreviews = [...photoPreviewUrls];
    URL.revokeObjectURL(newPreviews[index]); // Libérer la mémoire
    newPreviews.splice(index, 1);
    setPhotoPreviewUrls(newPreviews);
  };

  const MapClickHandler = () => {
    useMapEvents({
      click: handleMapClick
    });
    return null;
  };

  const getEntrepriseName = (idEntreprise) => {
    console.log('getEntrepriseName called with:', idEntreprise);
    console.log('entreprises array:', entreprises);
    const ent = entreprises.find(e => e.id === idEntreprise);
    console.log('found entreprise:', ent);
    return ent ? ent.nom : 'Non attribuée';
  };

  const getTypeLabel = (idType) => {
    const type = typesSignalement.find(t => t.id === idType);
    return type ? type.libelle : 'Type inconnu';
  };

  return (
    <div className="visitor-map-container">
      {localStorage.getItem('token') && (
        <button 
          className="logout-btn"
          onClick={handleLogout}
        >
          Déconnexion
        </button>
      )}

      {/* Barre de filtres */}
      <div className="filter-container">
        <div className="filter-group">
          <label htmlFor="filter-status">Statut:</label>
          <select 
            id="filter-status"
            value={filterStatus} 
            onChange={(e) => setFilterStatus(e.target.value)}
            className="filter-select"
          >
            <option value="tous">Tous les statuts</option>
            <option value="nouveau">Nouveau</option>
            <option value="en_cours">En cours</option>
            <option value="termine">Terminé</option>
          </select>
        </div>

        <div className="filter-group">
          <label htmlFor="filter-type">Type:</label>
          <select 
            id="filter-type"
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
        </div>

        <div className="filter-group">
          <label htmlFor="filter-entreprise">Entreprise:</label>
          <select 
            id="filter-entreprise"
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
        </div>

        {user && (
          <div className="filter-group filter-checkbox">
            <label>
              <input 
                type="checkbox" 
                checked={filterUser} 
                onChange={(e) => setFilterUser(e.target.checked)}
              />
              <span>Mes signalements uniquement</span>
            </label>
          </div>
        )}

        {(filterStatus !== 'tous' || filterType !== 'tous' || filterEntreprise !== 'tous' || filterUser) && (
          <button 
            className="filter-reset-btn"
            onClick={() => {
              setFilterStatus('tous');
              setFilterType('tous');
              setFilterEntreprise('tous');
              setFilterUser(false);
            }}
          >
            Réinitialiser les filtres
          </button>
        )}
      </div>

      <div className="stats-container">
        <div className="stat-card">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <div className="stat-value">{displayStats.total}</div>
            <div className="stat-label">Signalements</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <div className="stat-value">{displayStats.surfaceTotal} m²</div>
            <div className="stat-label">Surface totale</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <div className="stat-value">{formatCurrency(displayStats.budgetTotal)}</div>
            <div className="stat-label">Budget total</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <div className="stat-value">{displayStats.avancement}%</div>
            <div className="stat-label">Avancement</div>
          </div>
        </div>
      </div>

      <div className="map-wrapper">
        {loading ? (
          <div className="loading">Chargement de la carte...</div>
        ) : (
          <MapContainer
            center={position}
            zoom={17}
            style={{ height: '600px', width: '100%', borderRadius: '10px' }}
          >
            <TileLayer
              url="http://localhost:8081/styles/osm-bright/{z}/{x}/{y}.png"
              attribution='&copy; Carte locale Antananarivo'
            />
            
            <MapClickHandler />
            
            {filteredSignalements
              .filter(signal => [4, 5, 6].includes(signal.idStatus))
              .map((signal) => {
                let statusKey = '';
                if (signal.idStatus === 4) statusKey = 'nouveau';
                else if (signal.idStatus === 5) statusKey = 'en_cours';
                else if (signal.idStatus === 6) statusKey = 'termine';
                else statusKey = 'inconnu';
                return (
                  <Marker
                    key={signal.id}
                    position={[signal.latitude, signal.longitude]}
                    icon={getMarkerIcon(statusKey)}
                  >
                    <Popup>
                      <div className="popup-content">
                        <h3>{signal.typeSignalement?.libelle || 'Type inconnu'}</h3>
                        <div className="popup-info">
                          <p><strong>Date:</strong> {formatDate(signal.lastHistoriqueDate || signal.createdAt)}</p>
                          <p><strong>Statut:</strong> <span className={getStatusClass(statusKey)}>{getStatusLabel(statusKey)}</span></p>
                          <p><strong>Surface:</strong> {signal.surfaceM2} m²</p>
                          <p><strong>Budget:</strong> {formatCurrency(signal.budget)}</p>
                          <p><strong>Entreprise:</strong> {getEntrepriseName(signal.idEntreprise)}</p>
                        </div>
                      </div>
                    </Popup>
                  </Marker>
                );
              })}
          </MapContainer>
        )}
      </div>

      <div className="legend">
        <h3>Légende</h3>
        <div className="legend-items">
          <div className="legend-item">
            <span className="legend-marker nouveau"></span>
            <span>Nouveau</span>
          </div>
          <div className="legend-item">
            <span className="legend-marker en_cours"></span>
            <span>En cours</span>
          </div>
          <div className="legend-item">
            <span className="legend-marker termine"></span>
            <span>Terminé</span>
          </div>
        </div>
      </div>

      {/* Bouton flottant pour ajouter un signalement */}
      <button 
        className={`floating-add-btn ${isAddingMode ? 'active' : ''}`}
        onClick={() => setIsAddingMode(!isAddingMode)}
        title={isAddingMode ? 'Annuler' : 'Ajouter un signalement'}
      >
        {isAddingMode ? '✕' : '+'}
      </button>

      {/* Message d'info en mode ajout */}
      {isAddingMode && (
        <div className="floating-add-info">
           Cliquez sur la carte pour placer un signalement
        </div>
      )}

      {/* Modal pour ajouter un signalement */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Nouveau Signalement</h2>
            <p className="modal-coords">
               Position: {newSignalement.latitude.toFixed(5)}, {newSignalement.longitude.toFixed(5)}
            </p>
            
            <div className="modal-form">
              <div className="form-group">
                <label>Type de signalement *</label>
                <select
                  value={newSignalement.idTypeSignalement}
                  onChange={(e) => setNewSignalement({...newSignalement, idTypeSignalement: e.target.value})}
                  required
                >
                  <option value="">Sélectionnez un type</option>
                  {typesSignalement.map((type) => (
                    <option key={type.id} value={type.id}>
                      {type.libelle}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Type de probleme *</label>
                <textarea
                  value={newSignalement.description}
                  onChange={(e) => setNewSignalement({...newSignalement, description: e.target.value})}
                  placeholder="Décrivez le problème routier..."
                  rows="4"
                  required
                />
              </div>

              <div className="form-group">
                <label>Surface estimée (m²)</label>
                <input
                  type="number"
                  value={newSignalement.surface}
                  onChange={(e) => setNewSignalement({...newSignalement, surface: parseFloat(e.target.value)})}
                  placeholder="Surface en m²"
                  min="0"
                />
              </div>

              <div className="form-group">
                <label>Photos (max 5)</label>
                <div className="photo-upload-section">
                  <input
                    type="file"
                    id="photo-upload"
                    multiple
                    accept="image/*"
                    onChange={handlePhotoChange}
                    style={{ display: 'none' }}
                  />
                  <label htmlFor="photo-upload" className="photo-upload-btn">
                    📷 Ajouter des photos ({newSignalement.photos.length}/5)
                  </label>
                  
                  {photoPreviewUrls.length > 0 && (
                    <div className="photo-previews">
                      {photoPreviewUrls.map((url, index) => (
                        <div key={index} className="photo-preview-item">
                          <img src={url} alt={`Photo ${index + 1}`} />
                          <button
                            type="button"
                            className="remove-photo-btn"
                            onClick={() => handleRemovePhoto(index)}
                          >
                            ✕
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              <div className="modal-actions">
                <button className="btn-cancel" onClick={() => setShowModal(false)}>
                  Annuler
                </button>
                <button 
                  className="btn-submit" 
                  onClick={handleSubmitSignalement}
                  disabled={!newSignalement.description || !newSignalement.idTypeSignalement}
                >
                  Confirmer
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default VisitorMap;