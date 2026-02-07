import React, { useEffect, useState, useRef } from 'react';
import { auth } from '../firebase';
import { useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import { 
  FaCheckCircle, FaTimes, FaMapMarkerAlt, 
  FaPlus, FaMinus, FaInfoCircle, FaArrowRight,
  FaLayerGroup, FaClock, FaTools
} from 'react-icons/fa';
import { BiMoney } from 'react-icons/bi';
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
    termines: 0,
    nouveaux: 0,
    enCours: 0,
    surfaceTotal: 0,
    budgetTotal: 0,
    avancement: 0
  });
  const [loading, setLoading] = useState(true);
  const [typesSignalement, setTypesSignalement] = useState([]);
  const [entreprises, setEntreprises] = useState([]);
  const [user, setUser] = useState(null);
  const [filteredSignalements, setFilteredSignalements] = useState([]);
  const [showRecap, setShowRecap] = useState(false);
  const [showLegendExpanded, setShowLegendExpanded] = useState(false);

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
    window.location.href = '/';
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
    const termines = filteredSignalements.filter(s => s.idStatus === 6).length;
    const nouveaux = filteredSignalements.filter(s => s.idStatus === 4).length;
    const enCours = filteredSignalements.filter(s => s.idStatus === 5).length;
    const surfaceTotal = filteredSignalements.reduce((sum, s) => sum + (s.surfaceM2 || 0), 0);
    const budgetTotal = filteredSignalements.reduce((sum, s) => sum + (s.budget || 0), 0);
    const avancement = total > 0 ? Math.round((termines / total) * 100) : 0;

    setDisplayStats({
      total,
      termines,
      nouveaux,
      enCours,
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
      {/* Header avec boutons d'authentification */}
      <div className="map-header-actions">
        {localStorage.getItem('token') ? (
          <button 
            className="logout-btn"
            onClick={handleLogout}
          >
            Déconnexion
          </button>
        ) : (
          <button 
            className="login-btn"
            onClick={() => navigate('/login')}
          >
            Connexion Manager
          </button>
        )}
      </div>

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
          <div className="stat-icon-wrapper primary">
            <FaMapMarkerAlt className="stat-icon" />
          </div>
          <div className="stat-content">
            <div className="stat-value">{displayStats.total}</div>
            <div className="stat-label">Total</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper success">
            <FaCheckCircle className="stat-icon" />
          </div>
          <div className="stat-content">
            <div className="stat-value">{displayStats.termines}</div>
            <div className="stat-label">Terminé</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper warning">
            <FaClock className="stat-icon" />
          </div>
          <div className="stat-content">
            <div className="stat-value">{displayStats.nouveaux}</div>
            <div className="stat-label">Nouveau</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper info">
            <FaTools className="stat-icon" />
          </div>
          <div className="stat-content">
            <div className="stat-value">{displayStats.enCours}</div>
            <div className="stat-label">En cours</div>
          </div>
        </div>
      </div>

      {/* Bouton Récap Toggle */}
      <div className="recap-toggle" onClick={() => setShowRecap(!showRecap)}>
        <span className="recap-toggle-text">Récap</span>
        <span className="recap-toggle-state">{showRecap ? 'Masquer' : 'Afficher'}</span>
      </div>

      {/* Section Récap */}
      {showRecap && (
        <div className="recap-card">
          <div className="recap-content">
            <div className="recap-item">
              <span className="recap-label">Surface totale</span>
              <span className="recap-value">{displayStats.surfaceTotal.toFixed(0)} m²</span>
            </div>
            <div className="recap-item">
              <span className="recap-label">Budget total</span>
              <span className="recap-value">{formatCurrency(displayStats.budgetTotal)}</span>
            </div>
            <div className="recap-item">
              <span className="recap-label">Avancement</span>
              <span className="recap-value">{displayStats.avancement}%</span>
            </div>
          </div>
        </div>
      )}

      {/* Bannière d'information pour utilisateurs non connectés */}
      {!localStorage.getItem('token') && (
        <div className="info-banner">
          <div className="info-content">
            <FaInfoCircle className="info-icon" />
            <span>Connectez-vous pour signaler des problèmes sur la route</span>
          </div>
          <button className="info-action" onClick={() => navigate('/login')}>
            Se connecter
            <FaArrowRight />
          </button>
        </div>
      )}

      <div className="map-wrapper">
        {loading ? (
          <div className="loading-overlay">
            <div className="loading-content">
              <div className="loading-spinner"></div>
              <span>Chargement de la carte...</span>
            </div>
          </div>
        ) : (
          <>
          <MapContainer
            center={position}
            zoom={17}
            style={{ height: '600px', width: '100%', borderRadius: '10px' }}
          >
            <TileLayer
              url="http://localhost:8081/styles/osm-bright/{z}/{x}/{y}.png"
              attribution='&copy; Carte locale Antananarivo'
            />
            
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
                        {signal.photos && signal.photos.length > 0 && (
                          <div className="popup-photo">
                            <img 
                              src={signal.photos[0].url} 
                              alt="Photo du signalement" 
                              className="popup-photo-img"
                            />
                            {signal.photos.length > 1 && (
                              <span className="popup-photo-count">+{signal.photos.length - 1} photo{signal.photos.length > 2 ? 's' : ''}</span>
                            )}
                          </div>
                        )}
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

          {/* Contrôles de carte améliorés */}
          <div className="map-controls">
            <button 
              className="control-button zoom-button"
              onClick={() => {
                const map = document.querySelector('.leaflet-container')?._leaflet_map;
                if (map) map.zoomIn();
              }}
              title="Zoom +"
            >
              <FaPlus />
            </button>
            <button 
              className="control-button zoom-button"
              onClick={() => {
                const map = document.querySelector('.leaflet-container')?._leaflet_map;
                if (map) map.zoomOut();
              }}
              title="Zoom -"
            >
              <FaMinus />
            </button>
          </div>
          </>
        )}
      </div>

      {/* Légende flottante améliorée */}
      <div className={`map-legend ${showLegendExpanded ? 'expanded' : ''}`}>
        <button 
          className="legend-toggle"
          onClick={() => setShowLegendExpanded(!showLegendExpanded)}
        >
          <FaLayerGroup />
          <span className="legend-toggle-text">Légende</span>
        </button>
        {showLegendExpanded && (
          <div className="legend-items">
            <div className="legend-item">
              <div className="legend-marker nouveau">
                <span className="legend-marker-icon">●</span>
              </div>
              <span className="legend-text">Nouveau</span>
            </div>
            <div className="legend-item">
              <div className="legend-marker en_cours">
                <span className="legend-marker-icon">●</span>
              </div>
              <span className="legend-text">En cours</span>
            </div>
            <div className="legend-item">
              <div className="legend-marker termine">
                <span className="legend-marker-icon">●</span>
              </div>
              <span className="legend-text">Terminé</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default VisitorMap;