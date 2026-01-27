<template>
  <ion-page>
    <ion-header>
      <ion-toolbar class="custom-toolbar">
        <ion-title>
          <ion-icon name="map-outline" class="title-icon"></ion-icon>
          Carte des signalements
        </ion-title>
        <ion-buttons slot="end">
          <ion-button @click="toggleMyReports" class="filter-button">
            <ion-icon 
              :name="showMyReports ? 'globe-outline' : 'person-outline'" 
              slot="start"
            ></ion-icon>
            <span class="button-text">
              {{ showMyReports ? 'Tous' : 'Mes signalements' }}
            </span>
          </ion-button>
          <ion-button @click="goToLogin" fill="clear" class="login-button" v-if="!isAuthenticated">
            <ion-icon name="person-circle-outline" slot="icon-only"></ion-icon>
          </ion-button>
          <ion-button @click="handleLogout" fill="clear" class="logout-button" v-else>
            <ion-icon name="log-out-outline" slot="icon-only"></ion-icon>
          </ion-button>
        </ion-buttons>
      </ion-toolbar>

      <!-- Barre de stats -->
      <div class="stats-bar">
        <div class="stat-item">
          <ion-icon name="alert-circle-outline" class="stat-icon"></ion-icon>
          <div class="stat-content">
            <span class="stat-number">{{ totalReports }}</span>
            <span class="stat-label">Total</span>
          </div>
        </div>
        <div class="stat-item">
          <ion-icon name="checkmark-circle-outline" class="stat-icon success"></ion-icon>
          <div class="stat-content">
            <span class="stat-number">{{ approvedReports }}</span>
            <span class="stat-label">Validés</span>
          </div>
        </div>
        <div class="stat-item">
          <ion-icon name="time-outline" class="stat-icon warning"></ion-icon>
          <div class="stat-content">
            <span class="stat-number">{{ pendingReports }}</span>
            <span class="stat-label">En attente</span>
          </div>
        </div>
      </div>

      <!-- Message d'information pour les utilisateurs non connectés -->
      <div v-if="!isAuthenticated" class="info-banner">
        <ion-icon name="information-circle-outline" class="info-icon"></ion-icon>
        <span>Connectez-vous pour signaler des problèmes sur la route</span>
      </div>
    </ion-header>

    <ion-content>
      <!-- Carte -->
      <div id="map" ref="mapContainer"></div>

      <!-- Légende flottante -->
      <div class="map-legend">
        <div class="legend-item">
          <div class="legend-marker trou"></div>
          <span>Trou</span>
        </div>
        <div class="legend-item">
          <div class="legend-marker chantier"></div>
          <span>Chantier</span>
        </div>
        <div class="legend-item">
          <div class="legend-marker deviation"></div>
          <span>Déviation</span>
        </div>
      </div>

      <!-- Bouton localisation -->
      <ion-fab vertical="top" horizontal="end" slot="fixed" class="location-fab">
        <ion-fab-button
          size="small"
          @click="centerOnUser"
          @dblclick="recenterOnGPS"
          :class="['location-button', { 'tracking-active': isTrackingLocation }]"
          title="Clic: Activer/désactiver suivi GPS | Double-clic: Recentrer"
        >
          <ion-icon :name="isTrackingLocation ? 'locate' : 'locate-outline'"></ion-icon>
        </ion-fab-button>
      </ion-fab>    
      <!-- Indicateur GPS actif -->
      <div v-if="isTrackingLocation && userLocationMarker" class="gps-indicator">
        <ion-icon name="radio-button-on-outline" class="gps-icon active"></ion-icon>
        <span class="gps-text">GPS actif</span>
        <ion-button 
          fill="clear" 
          size="small" 
          @click="recenterOnGPS"
          class="recenter-btn"
          title="Recentrer sur ma position"
        >
          <ion-icon name="locate-outline" slot="icon-only"></ion-icon>
        </ion-button>
      </div>

      <!-- Bouton Ajouter -->
      <ion-fab vertical="bottom" horizontal="end" slot="fixed">
        <ion-fab-button @click="startAddReport" class="add-button">
          <ion-icon name="add-outline"></ion-icon>
        </ion-fab-button>
      </ion-fab>

      <!-- Message si aucun signalement -->
      <div v-if="totalReports === 0" class="empty-state">
        <ion-icon name="location-outline" class="empty-icon"></ion-icon>
        <h3>Aucun signalement</h3>
        <p>Soyez le premier à signaler un problème</p>
      </div>
    </ion-content>

    <!-- Modal formulaire signalement -->
    <ion-modal :is-open="showForm" @didDismiss="closeForm">
      <ion-header>
        <ion-toolbar class="custom-toolbar">
          <ion-title>Nouveau signalement</ion-title>
          <ion-buttons slot="end">
            <ion-button @click="closeForm" fill="clear">
              <ion-icon name="close-outline"></ion-icon>
            </ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>
      
      <ion-content class="ion-padding modal-content">
        <div class="form-container">
          <!-- Preview position sur mini carte -->
          <div class="position-preview">
            <ion-icon name="location-outline" class="position-icon"></ion-icon>
            <div class="position-info">
              <span class="position-label">Position sélectionnée</span>
              <span class="position-coords">
                {{ newPosition.lat.toFixed(5) }}, {{ newPosition.lng.toFixed(5) }}
              </span>
            </div>
          </div>

          <ion-card class="form-card">
            <ion-card-content>
              <!-- Type de signalement -->
              <div class="form-group">
                <ion-label class="form-label">
                  <ion-icon name="list-outline"></ion-icon>
                  Type de signalement *
                </ion-label>
                <ion-segment v-model="reportType" class="type-segment">
                  <ion-segment-button value="trou">
                    <ion-icon name="alert-circle-outline"></ion-icon>
                    <ion-label>Trou</ion-label>
                  </ion-segment-button>
                  <ion-segment-button value="chantier">
                    <ion-icon name="construct-outline"></ion-icon>
                    <ion-label>Chantier</ion-label>
                  </ion-segment-button>
                  <ion-segment-button value="deviation">
                    <ion-icon name="swap-horizontal-outline"></ion-icon>
                    <ion-label>Déviation</ion-label>
                  </ion-segment-button>
                </ion-segment>
              </div>

              <!-- Description -->
              <div class="form-group">
                <ion-label class="form-label">
                  <ion-icon name="document-text-outline"></ion-icon>
                  Description *
                </ion-label>
                <ion-textarea
                  v-model="reportDescription"
                  placeholder="Décrivez le problème en détail..."
                  :rows="4"
                  class="description-textarea"
                  :counter="true"
                  :maxlength="500"
                ></ion-textarea>
              </div>

              <!-- Instructions -->
              <ion-note class="instruction-note">
                <ion-icon name="information-circle-outline"></ion-icon>
                Cliquez sur la carte pour ajuster la position si nécessaire
              </ion-note>

              <!-- Boutons -->
              <div class="button-group">
                <ion-button 
                  expand="block" 
                  @click="submitReport"
                  :disabled="!canSubmit || submitting"
                  class="submit-button"
                >
                  <ion-spinner name="crescent" v-if="submitting"></ion-spinner>
                  <span v-else>
                    <ion-icon name="checkmark-outline" slot="start"></ion-icon>
                    Soumettre le signalement
                  </span>
                </ion-button>

                <ion-button 
                  expand="block" 
                  fill="outline"
                  @click="closeForm"
                  class="cancel-button"
                >
                  Annuler
                </ion-button>
              </div>
            </ion-card-content>
          </ion-card>
        </div>
      </ion-content>
    </ion-modal>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import {
  IonPage, IonHeader, IonToolbar, IonTitle, IonButtons, IonButton,
  IonContent, IonFab, IonFabButton, IonFabList, IonIcon, IonModal, IonLabel,
  IonSegment, IonSegmentButton, IonTextarea, IonCard, IonCardContent,
  IonNote, IonSpinner, toastController
} from '@ionic/vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { collection, addDoc, getDocs, query, where } from 'firebase/firestore';
import { db, auth } from '@/services/firebase';
import { logout } from '@/services/auth.service';
import { startLocationTracking, stopLocationTracking, getCurrentLocation, type Location } from '@/services/location.service';
import { onAuthStateChanged } from 'firebase/auth';

// Correction des icônes par défaut de Leaflet
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

// --- Références et états ---
const router = useRouter();
const map = ref<L.Map>();
const mapContainer = ref<HTMLElement>();
const markers: L.Marker[] = [];

const showMyReports = ref(false);
const showForm = ref(false);
const submitting = ref(false);

const reportDescription = ref('');
const reportType = ref('trou');
const newPosition = ref({ lat: -18.9062, lng: 47.5282 }); // centre d'Antananarivo

const totalReports = ref(0);
const approvedReports = ref(0);
const pendingReports = ref(0);

// État d'authentification
const isAuthenticated = ref(false);

// Suivi GPS
const isTrackingLocation = ref(false);
const userLocationMarker = ref<L.Marker>();
const currentLocation = ref<Location>();
const isUserInteracting = ref(false);
const pendingLocationUpdate = ref<Location | null>(null);

// --- Computed ---
const canSubmit = computed(() => {
  return reportDescription.value.trim().length > 0 && reportType.value !== '';
});

// --- Toast helper ---
const showToast = async (message: string, color: string = 'success') => {
  const toast = await toastController.create({
    message,
    duration: 2000,
    position: 'top',
    color
  });
  await toast.present();
};

// --- Filtrer mes signalements ---
const toggleMyReports = () => {
  // Vérifier si la page est visible pour éviter les problèmes d'accessibilité
  if (document.hidden) return;

  // Si l'utilisateur n'est pas connecté et essaie de voir "mes signalements"
  if (!isAuthenticated.value) {
    showToast('Veuillez vous connecter pour voir vos signalements', 'warning');
    router.push('/login');
    return;
  }

  showMyReports.value = !showMyReports.value;
  loadReports();
};

// --- Fallback géolocalisation par IP ---
// --- Centrer sur l'utilisateur et activer/désactiver le suivi GPS ---
const centerOnUser = async () => {
  // Vérifier si la page est visible pour éviter les problèmes d'accessibilité
  if (document.hidden) return;

  if (!map.value) {
    console.warn('Carte non initialisée');
    showToast('Carte non initialisée', 'danger');
    return;
  }

  if (isTrackingLocation.value) {
    // Désactiver le suivi GPS
    stopLocationTracking();
    isTrackingLocation.value = false;

    // Supprimer le marker de position
    if (userLocationMarker.value && map.value.hasLayer(userLocationMarker.value)) {
      map.value.removeLayer(userLocationMarker.value);
      userLocationMarker.value = undefined;
    }

    showToast('Suivi GPS désactivé', 'warning');
    return;
  }

  // Vérifier si la géolocalisation est supportée
  if (!navigator.geolocation) {
    showToast('La géolocalisation n\'est pas supportée par ce navigateur', 'danger');
    return;
  }

  // Activer le suivi GPS
  showToast('Activation du suivi GPS...', 'primary');

  try {
    // Vérifier les permissions en essayant d'obtenir une position
    await getCurrentLocation();
    
    // Démarrer le suivi continu
    startLocationTracking((location: Location) => {
      updateUserLocation(location);
    });

    isTrackingLocation.value = true;
    showToast('Suivi GPS activé - Position mise à jour en temps réel', 'success');

    // Centrer immédiatement sur la position actuelle si l'utilisateur n'interagit pas
    if (!isUserInteracting.value && currentLocation.value) {
      applyLocationUpdate(currentLocation.value);
    }

  } catch (error) {
    console.error('Erreur d\'activation du suivi GPS:', error);
    const errorMessage = error instanceof Error ? error.message : 'Erreur inconnue';
    showToast(`Erreur GPS: ${errorMessage}`, 'danger');
    
    // Suggestions pour résoudre les problèmes
    if (errorMessage.includes('Permission')) {
      showToast('Vérifiez les permissions de géolocalisation dans les paramètres du navigateur', 'warning');
    } else if (errorMessage.includes('Position indisponible')) {
      showToast('Position GPS indisponible. Vérifiez votre connexion GPS.', 'warning');
    }
  }
};

// --- Mettre à jour la position utilisateur ---
const updateUserLocation = (location: Location) => {
  if (!map.value) return;

  // Si l'utilisateur est en train d'interagir, stocker la mise à jour pour plus tard
  if (isUserInteracting.value) {
    pendingLocationUpdate.value = location;
    return;
  }

  // Appliquer la mise à jour normalement
  applyLocationUpdate(location);
};

// --- Appliquer une mise à jour de position ---
const applyLocationUpdate = (location: Location) => {
  if (!map.value) return;

  currentLocation.value = location;

  // Créer ou mettre à jour le marker de position
  if (!userLocationMarker.value) {
    // Premier marker - centrer la carte seulement si ce n'est pas une interaction utilisateur
    if (!isUserInteracting.value) {
      const currentZoom = map.value.getZoom();
      map.value.flyTo([location.latitude, location.longitude], currentZoom, {
        duration: 1.5,
        easeLinearity: 0.25
      });
    }

    // Créer le marker permanent
    userLocationMarker.value = L.marker([location.latitude, location.longitude], {
      icon: L.divIcon({
        html: '<div style="background: #3b82f6; width: 24px; height: 24px; border-radius: 50%; border: 3px solid white; box-shadow: 0 2px 8px rgba(0,0,0,0.4); position: relative;"><div style="position: absolute; top: 2px; left: 2px; width: 16px; height: 16px; background: white; border-radius: 50%; border: 2px solid #3b82f6;"></div></div>',
        className: '',
        iconSize: [24, 24],
        iconAnchor: [12, 12]
      })
    }).bindPopup('Votre position actuelle (suivi actif)');

    userLocationMarker.value.addTo(map.value);
  } else {
    // Mettre à jour la position du marker existant
    userLocationMarker.value.setLatLng([location.latitude, location.longitude]);
    
    // Centrer la carte sur la nouvelle position seulement si le suivi est actif ET qu'il n'y a pas d'interaction utilisateur
    if (isTrackingLocation.value && !isUserInteracting.value) {
      map.value.panTo([location.latitude, location.longitude], {
        animate: true,
        duration: 0.5
      });
    }
  }
};

// --- Commencer ajout signalement ---
const startAddReport = () => {
  // Vérifier si la page est visible pour éviter les problèmes d'accessibilité
  if (document.hidden) return;

  showForm.value = true;
};

// --- Commencer ajout signalement avec position ---
const startAddReportAtPosition = (position: L.LatLng) => {
  // Vérifier si la page est visible pour éviter les problèmes d'accessibilité
  if (document.hidden) return;

  // Mettre à jour la position
  newPosition.value = position;
  
  // Ouvrir le formulaire
  showForm.value = true;
  
  showToast('Position sélectionnée - Remplissez le formulaire', 'primary');
};

// --- Fermer le formulaire ---
const closeForm = () => {
  showForm.value = false;
  reportDescription.value = '';
  reportType.value = 'trou';
};

// --- Soumettre signalement ---
const submitReport = async () => {
  // Vérifier si la page est visible pour éviter les problèmes d'accessibilité
  if (document.hidden) return;

  // Vérifier l'authentification avant de permettre la soumission
  if (!isAuthenticated.value) {
    showToast('Veuillez vous connecter pour signaler un problème', 'warning');
    router.push('/login');
    return;
  }

  if (!canSubmit.value) return;

  submitting.value = true;

  try {
    await addDoc(collection(db, 'reports'), {
      uid: auth.currentUser?.uid,
      description: reportDescription.value,
      type: reportType.value,
      lat: newPosition.value.lat,
      lng: newPosition.value.lng,
      status: 'pending',
      createdAt: new Date()
    });

    showToast('Signalement créé avec succès!');
    closeForm();
    loadReports();
  } catch (error) {
    console.error('Erreur lors de la création:', error);
    showToast('Erreur lors de la création du signalement', 'danger');
  } finally {
    submitting.value = false;
  }
};

// --- Aller à la page de connexion ---
const goToLogin = () => {
  router.push('/login');
};


// --- Recentrer manuellement sur la position GPS ---
const recenterOnGPS = () => {
  if (currentLocation.value && map.value) {
    // Forcer l'application de la mise à jour même si on interagit
    const wasInteracting = isUserInteracting.value;
    isUserInteracting.value = false; // Temporairement désactiver pour permettre le centrage
    
    applyLocationUpdate(currentLocation.value);
    
    // Remettre l'état d'interaction si nécessaire
    if (wasInteracting) {
      setTimeout(() => {
        isUserInteracting.value = true;
      }, 1000); // Laisser le temps au centrage de se faire
    }
    
    showToast('Carte recentrée sur votre position', 'success');
  } else {
    showToast('Position GPS non disponible', 'warning');
  }
};

// --- Icônes personnalisées par type ---
const getMarkerIcon = (type: string) => {
  const colors: Record<string, string> = {
    trou: '#ef4444',
    chantier: '#f59e0b',
    deviation: '#3b82f6'
  };

  return L.divIcon({
    html: `
      <div style="
        background: ${colors[type] || '#64748b'};
        width: 32px;
        height: 32px;
        border-radius: 50% 50% 50% 0;
        border: 3px solid white;
        box-shadow: 0 4px 12px rgba(0,0,0,0.3);
        transform: rotate(-45deg);
        display: flex;
        align-items: center;
        justify-content: center;
      ">
        <div style="transform: rotate(45deg); color: white; font-size: 16px; font-weight: bold;">
          ${type === 'trou' ? '!' : type === 'chantier' ? '⚙' : '↔'}
        </div>
      </div>
    `,
    className: '',
    iconSize: [32, 32],
    iconAnchor: [16, 32]
  });
};

// --- Charger signalements depuis Firestore ---
const loadReports = async () => {
  if (!map.value) return;

  // Supprimer anciens markers
  markers.forEach(m => {
    try {
      map.value?.removeLayer(m);
    } catch (e) {
      console.warn('Erreur suppression marker:', e);
    }
  });
  markers.length = 0;

  try {
    const reportsRef = collection(db, 'reports');
    let q;

    // Si "Mes signalements" est activé ET que l'utilisateur est connecté
    if (showMyReports.value && isAuthenticated.value && auth.currentUser) {
      q = query(reportsRef, where('uid', '==', auth.currentUser.uid));
    } else {
      // Sinon, afficher tous les signalements
      q = reportsRef;
    }

    const snapshot = await getDocs(q);
    
    let approved = 0;
    let pending = 0;

    snapshot.forEach(doc => {
      const data: any = doc.data();
      
      if (data.status === 'approved') approved++;
      if (data.status === 'pending') pending++;

      // Afficher seulement les signalements validés
      if (data.status === 'approved' && map.value && data.lat && data.lng) {
        const marker = L.marker([data.lat, data.lng], {
          icon: getMarkerIcon(data.type)
        }).bindPopup(`
          <div style="min-width: 200px;">
            <h4 style="margin: 0 0 8px 0; color: #1e3a8a; text-transform: capitalize;">
              ${data.type}
            </h4>
            <p style="margin: 0 0 8px 0;">${data.description}</p>
            <div style="display: flex; gap: 8px; font-size: 12px; color: #64748b;">
              <span>📅 ${new Date(data.createdAt?.toDate()).toLocaleDateString('fr-FR')}</span>
              <span style="
                padding: 2px 8px;
                background: #dcfce7;
                color: #166534;
                border-radius: 12px;
              ">
                Validé
              </span>
            </div>
          </div>
        `);
        
        marker.addTo(map.value);
        markers.push(marker);
      }
    });

    totalReports.value = snapshot.size;
    approvedReports.value = approved;
    pendingReports.value = pending;
  } catch (error) {
    console.error('Erreur chargement reports:', error);
    showToast('Erreur lors du chargement des signalements', 'danger');
  }
};

// --- Initialisation carte ---
onMounted(async () => {
  // Écouter les changements d'authentification
  onAuthStateChanged(auth, (user) => {
    isAuthenticated.value = !!user;
    console.log('État d\'authentification mis à jour:', isAuthenticated.value);
  });

  if (!mapContainer.value) return;

  // Attendre un petit délai pour que le DOM soit complètement rendu
  await new Promise(resolve => setTimeout(resolve, 100));

  if (!mapContainer.value) return;

  try {
    map.value = L.map(mapContainer.value, {
      zoomControl: true,
      attributionControl: true
    }).setView([newPosition.value.lat, newPosition.value.lng], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(map.value);

    // Click pour définir la position du nouveau report
    map.value.on('click', (e: L.LeafletMouseEvent) => {
      if (showForm.value) {
        // Si le formulaire est ouvert, mettre à jour la position
        newPosition.value = e.latlng;
        showToast('Position mise à jour', 'primary');
      } else {
        // Si le formulaire n'est pas ouvert, proposer de créer un signalement
        const confirmCreate = confirm(`Voulez-vous créer un signalement à cette position ?\nLatitude: ${e.latlng.lat.toFixed(5)}\nLongitude: ${e.latlng.lng.toFixed(5)}`);
        if (confirmCreate) {
          startAddReportAtPosition(e.latlng);
        }
      }
    });

    // Détecter les interactions utilisateur (zoom, déplacement)
    map.value.on('zoomstart movestart', () => {
      isUserInteracting.value = true;
    });

    map.value.on('zoomend moveend', () => {
      // Petit délai pour s'assurer que l'interaction est terminée
      setTimeout(() => {
        isUserInteracting.value = false;
        
        // Appliquer les mises à jour de position en attente
        if (pendingLocationUpdate.value) {
          applyLocationUpdate(pendingLocationUpdate.value);
          pendingLocationUpdate.value = null;
        }
      }, 150); // Augmenter le délai pour être sûr
    });

    await loadReports();
  } catch (error) {
    console.error('Erreur initialisation carte:', error);
    showToast('Erreur lors du chargement de la carte', 'danger');
  }
});

// --- Déconnexion ---
const handleLogout = async () => {
  // Vérifier si la page est visible pour éviter les problèmes d'accessibilité
  if (document.hidden) return;

  try {
    await logout();
    showToast('Déconnexion réussie', 'success');
    router.push('/login');
  } catch (error) {
    console.error('Erreur lors de la déconnexion:', error);
    showToast('Erreur lors de la déconnexion', 'danger');
  }
};

// --- Nettoyage ---
onBeforeUnmount(() => {
  // Arrêter le suivi GPS
  if (isTrackingLocation.value) {
    stopLocationTracking();
    isTrackingLocation.value = false;
  }

  if (map.value) {
    map.value.remove();
  }
});
</script>

<style scoped>
/* Toolbar */
.custom-toolbar {
  --background: linear-gradient(135deg, #0a1929 0%, #1e3a8a 100%);
  --color: white;
}

.title-icon {
  font-size: 20px;
  margin-right: 8px;
  vertical-align: middle;
}

.filter-button {
  --color: white;
  --background: rgba(255, 255, 255, 0.1);
  --border-radius: 20px;
  font-size: 14px;
  text-transform: none;
}

.logout-button {
  --color: white;
  --padding-start: 8px;
  --padding-end: 8px;
  margin-left: 8px;
}

.button-text {
  margin-left: 4px;
}

/* Barre de stats */
.stats-bar {
  display: flex;
  justify-content: space-around;
  padding: 12px 16px;
  background: linear-gradient(180deg, #0a1929 0%, #1a2332 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-icon {
  font-size: 24px;
  color: #3b82f6;
}

.stat-icon.success {
  color: #22c55e;
}

.stat-icon.warning {
  color: #f59e0b;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 18px;
  font-weight: 700;
  color: white;
}

.stat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  text-transform: uppercase;
}

/* Bannière d'information */
.info-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  font-size: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.info-icon {
  font-size: 18px;
  flex-shrink: 0;
}

/* Carte */
#map {
  height: 100%;
  width: 100%;
}

/* Légende */
.map-legend {
  position: absolute;
  bottom: 80px;
  left: 16px;
  background: white;
  padding: 12px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  z-index: 1000;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
}

.legend-item:last-child {
  margin-bottom: 0;
}

.legend-marker {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.legend-marker.trou {
  background: #ef4444;
}

.legend-marker.chantier {
  background: #f59e0b;
}

.legend-marker.deviation {
  background: #3b82f6;
}

/* FAB Buttons */
.location-fab {
  top: 180px;
}

.location-button {
  --background: white;
  --color: #1e3a8a;
}

.location-button.tracking-active {
  --background: #3b82f6;
  --color: white;
  --box-shadow: 0 4px 16px rgba(59, 130, 246, 0.6);
}

.add-button {
  --background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
  --box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4);
}

.add-button-small {
  --background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
  --box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4);
}

.info-button {
  --background: #64748b;
  --color: white;
}

/* Message d'aide */
.help-message {
  position: absolute;
  top: 120px;
  right: 16px;
  background: white;
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #1e3a8a;
  max-width: 250px;
  animation: fadeIn 0.5s ease-in-out;
}

.help-icon {
  font-size: 20px;
  color: #3b82f6;
  flex-shrink: 0;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Indicateur GPS */
.gps-indicator {
  position: absolute;
  top: 240px;
  right: 16px;
  background: rgba(59, 130, 246, 0.95);
  color: white;
  padding: 8px 12px;
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  backdrop-filter: blur(10px);
  animation: fadeIn 0.3s ease-out;
}

.gps-icon.active {
  color: #22c55e;
  animation: pulse 2s infinite;
}

.gps-text {
  font-weight: 500;
}

.recenter-btn {
  --color: white;
  --padding-start: 4px;
  --padding-end: 4px;
  margin-left: 4px;
}

.recenter-btn ion-icon {
  font-size: 16px;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

/* Modal */
.modal-content {
  --background: #f8fafc;
}

.form-container {
  max-width: 600px;
  margin: 0 auto;
}

/* Position Preview */
.position-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.position-icon {
  font-size: 32px;
  color: #3b82f6;
}

.position-info {
  display: flex;
  flex-direction: column;
}

.position-label {
  font-size: 12px;
  color: #64748b;
  text-transform: uppercase;
  font-weight: 600;
}

.position-coords {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
  font-family: monospace;
}

/* Form Card */
.form-card {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-radius: 16px;
}

.form-group {
  margin-bottom: 24px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
}

.form-label ion-icon {
  color: #3b82f6;
}

.type-segment {
  --background: #f1f5f9;
}

.type-segment ion-segment-button {
  min-height: 60px;
  text-transform: none;
}

.description-textarea {
  --background: #f8fafc;
  --border-color: #e2e8f0;
  --border-radius: 12px;
  --padding-start: 16px;
  --padding-end: 16px;
  --padding-top: 12px;
  --padding-bottom: 12px;
}

.instruction-note {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #eff6ff;
  border-left: 4px solid #3b82f6;
  border-radius: 8px;
  font-size: 13px;
  color: #1e40af;
  margin-bottom: 24px;
}

.button-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.submit-button {
  --background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
  --border-radius: 12px;
  height: 50px;
  font-weight: 600;
}

.cancel-button {
  --border-color: #cbd5e1;
  --color: #64748b;
  height: 50px;
  text-transform: none;
}

/* Responsive */
@media (max-width: 768px) {
  .button-text {
    display: none;
  }

  .stats-bar {
    padding: 8px 12px;
  }

  .stat-number {
    font-size: 16px;
  }

  .stat-label {
    font-size: 10px;
  }
}
</style>