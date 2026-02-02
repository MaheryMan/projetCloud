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

// Icônes personnalisées selon le statut
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
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [newSignalement, setNewSignalement] = useState({
    latitude: 0,
    longitude: 0,
    description: '',
    surface: 0,
    idTypeSignalement: ''
  });
  const [isAddingMode, setIsAddingMode] = useState(false);
  const [typesSignalement, setTypesSignalement] = useState([]);
  const [entreprises, setEntreprises] = useState([]);

  const position = [-18.8792, 47.5079];

  const getAuthToken = async () => {
    // Prend le token Firebase si connecté, sinon fallback sur le token backend
    if (auth.currentUser) {
      return await auth.currentUser.getIdToken();
    }
    return localStorage.getItem('token');
  };

  const fetchEntreprises = async () => {
    try {
      const token = await getAuthToken();
      const headers = token ? { 'Authorization': `Bearer ${token}` } : {};
      const response = await fetch('http://localhost:8080/api/entreprises', { headers });
      if (!response.ok) throw new Error('Erreur de chargement des entreprises');
      const data = await response.json();
      setEntreprises(data);
    } catch (error) {
      console.error('Erreur:', error);
    }
  };

  useEffect(() => {
    fetchSignalements();
    fetchTypesSignalement();
    fetchEntreprises();
  }, []);

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
      const response = await fetch('http://localhost:8080/api/signalements', { headers });
      if (!response.ok) throw new Error('Erreur de chargement');
      const data = await response.json();
      setSignalements(data);
      calculateStats(data);
    } catch (error) {
      console.error('Erreur:', error);
    } finally {
      setLoading(false);
    }
  };

  const calculateStats = (data) => {
    const total = data.length;
    const surfaceTotal = data.reduce((sum, s) => sum + (s.surface || 0), 0);
    const budgetTotal = data.reduce((sum, s) => sum + (s.budget || 0), 0);
    const termines = data.filter(s => s.status === 'termine').length;
    const avancement = total > 0 ? Math.round((termines / total) * 100) : 0;

    setStats({ total, surfaceTotal, budgetTotal, avancement });
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
          idTypeSignalement: newSignalement.idTypeSignalement,
          idStatus: 1, // Statut "nouveau" par défaut
          idEntreprise: null,
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
      setNewSignalement({ latitude: 0, longitude: 0, description: '', surface: 0, idTypeSignalement: '' });
      await fetchSignalements();
    } catch (error) {
      console.error('Erreur:', error);
      alert('❌ Erreur lors de l\'ajout du signalement: ' + error.message);
    }
  };

  const MapClickHandler = () => {
    useMapEvents({
      click: handleMapClick
    });
    return null;
  };

  const getEntrepriseName = (idEntreprise) => {
    const ent = entreprises.find(e => e.id === idEntreprise);
    return ent ? ent.nom : 'Non attribuée';
  };

  return (
    <div className="visitor-map-container">
      <header className="map-header">
        <h1>Travaux Routiers - Antananarivo</h1>
        <p>Suivi des problèmes routiers en temps réel</p>
        
        <button 
          className={`add-signalement-btn ${isAddingMode ? 'active' : ''}`}
          onClick={() => setIsAddingMode(!isAddingMode)}
        >
          {isAddingMode ? ' Annuler' : ' Ajouter un signalement'}
        </button>
        
        {isAddingMode && (
          <div className="add-mode-info">
             Cliquez sur la carte pour placer un signalement
          </div>
        )}
      </header>

      <div className="stats-container">
        <div className="stat-card">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <div className="stat-value">{stats.total}</div>
            <div className="stat-label">Signalements</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <div className="stat-value">{stats.surfaceTotal} m²</div>
            <div className="stat-label">Surface totale</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <div className="stat-value">{formatCurrency(stats.budgetTotal)}</div>
            <div className="stat-label">Budget total</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon"></div>
          <div className="stat-content">
            <div className="stat-value">{stats.avancement}%</div>
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
            zoom={13}
            style={{ height: '600px', width: '100%', borderRadius: '10px' }}
          >
            <TileLayer
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            />
            
            <MapClickHandler />
            
            {signalements
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
                        <h3>Signalement #{signal.id}</h3>
                        <div className="popup-info">
                          <p><strong>Date:</strong> {formatDate(signal.created_at)}</p>
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
