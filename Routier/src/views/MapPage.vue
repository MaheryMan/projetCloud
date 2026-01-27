<template>
  <ion-page>
    <ion-header>
      <ion-toolbar class="custom-toolbar">
        <ion-title>
          <div class="title-container">
            <div class="title-icon-wrapper">
              <ion-icon name="map-outline" class="title-icon"></ion-icon>
              <span class="title-pulse"></span>
            </div>
            <span class="title-text">Carte des signalements</span>
          </div>
        </ion-title>
        <ion-buttons slot="end">
          <ion-button @click="toggleMyReports" class="filter-button" :class="{ 'active': showMyReports }">
            <ion-icon 
              :name="showMyReports ? 'globe-outline' : 'person-outline'" 
              slot="start"
            ></ion-icon>
            <span class="button-text">
              {{ showMyReports ? 'Tous' : 'Mes signalements' }}
            </span>
          </ion-button>
          <ion-button @click="goToLogin" fill="clear" class="auth-button" v-if="!isAuthenticated">
            <ion-icon name="person-circle-outline" slot="icon-only"></ion-icon>
          </ion-button>
          <ion-button @click="handleLogout" fill="clear" class="auth-button logout" v-else>
            <ion-icon name="log-out-outline" slot="icon-only"></ion-icon>
          </ion-button>
        </ion-buttons>
      </ion-toolbar>

      <!-- Barre de stats améliorée -->
      <div class="stats-bar">
        <div class="stat-item" v-for="stat in statsData" :key="stat.label">
          <div class="stat-icon-wrapper" :class="stat.class">
            <ion-icon :name="stat.icon" class="stat-icon"></ion-icon>
          </div>
          <div class="stat-content">
            <span class="stat-number">
              <span class="number-value">{{ stat.value }}</span>
            </span>
            <span class="stat-label">{{ stat.label }}</span>
          </div>
        </div>
      </div>

      <!-- Message d'information amélioré -->
      <transition name="slide-fade">
        <div v-if="!isAuthenticated" class="info-banner">
          <div class="info-content">
            <ion-icon name="information-circle-outline" class="info-icon"></ion-icon>
            <span>Connectez-vous pour signaler des problèmes sur la route</span>
          </div>
          <ion-button fill="clear" size="small" @click="goToLogin" class="info-action">
            Se connecter
            <ion-icon name="arrow-forward-outline" slot="end"></ion-icon>
          </ion-button>
        </div>
      </transition>
    </ion-header>

    <ion-content>
      <!-- Carte -->
      <div id="map" ref="mapContainer"></div>

      <!-- Légende flottante améliorée -->
      <transition name="slide-up">
        <div class="map-legend" v-if="!showForm && showLegend">
          <button class="legend-toggle" @click="toggleLegend">
            <ion-icon name="layers-outline"></ion-icon>
            <span class="legend-toggle-text">Légende</span>
          </button>
          <div class="legend-items" v-if="isLegendExpanded">
            <div class="legend-item" v-for="item in legendItems" :key="item.type">
              <div class="legend-marker" :class="item.type">
                <span class="legend-marker-icon">{{ item.icon }}</span>
              </div>
              <span class="legend-text">{{ item.label }}</span>
            </div>
          </div>
        </div>
      </transition>

      <!-- Contrôles de carte améliorés -->
      <div class="map-controls">
        <!-- Bouton localisation -->
        <div class="control-button-wrapper">
          <ion-fab-button
            size="small"
            @click="centerOnUser"
            @dblclick="recenterOnGPS"
            :class="['control-button', 'location-button', { 'active': isTrackingLocation }]"
            title="Clic: Activer/désactiver suivi GPS"
          >
            <ion-icon :name="isTrackingLocation ? 'locate' : 'locate-outline'"></ion-icon>
          </ion-fab-button>
          <span class="control-tooltip">GPS</span>
        </div>

        <!-- Bouton zoom + -->
        <div class="control-button-wrapper">
          <ion-fab-button
            size="small"
            @click="zoomIn"
            class="control-button zoom-button"
          >
            <ion-icon name="add-outline"></ion-icon>
          </ion-fab-button>
          <span class="control-tooltip">Zoom +</span>
        </div>

        <!-- Bouton zoom - -->
        <div class="control-button-wrapper">
          <ion-fab-button
            size="small"
            @click="zoomOut"
            class="control-button zoom-button"
          >
            <ion-icon name="remove-outline"></ion-icon>
          </ion-fab-button>
          <span class="control-tooltip">Zoom -</span>
        </div>
      </div>

      <!-- Indicateur GPS compact -->
      <transition name="fade-slide">
        <div v-if="isTrackingLocation && userLocationMarker" class="gps-indicator">
          <span class="gps-dot"></span>
          <span class="gps-text">GPS</span>
        </div>
      </transition>

      <!-- Bouton Ajouter amélioré -->
      <ion-fab vertical="bottom" horizontal="end" slot="fixed">
        <ion-fab-button @click="startAddReport" class="add-button">
          <ion-icon name="add-outline" class="add-icon"></ion-icon>
          <span class="add-pulse"></span>
        </ion-fab-button>
      </ion-fab>

      <!-- Message si aucun signalement amélioré -->
      <transition name="fade-up">
        <div v-if="totalReports === 0" class="empty-state">
          <div class="empty-illustration">
            <ion-icon name="location-outline" class="empty-icon"></ion-icon>
            <div class="empty-circles">
              <span class="circle circle-1"></span>
              <span class="circle circle-2"></span>
              <span class="circle circle-3"></span>
            </div>
          </div>
          <h3 class="empty-title">Aucun signalement</h3>
          <p class="empty-description">Soyez le premier à signaler un problème sur la route</p>
          <ion-button fill="outline" @click="startAddReport" class="empty-action">
            <ion-icon name="add-circle-outline" slot="start"></ion-icon>
            Créer un signalement
          </ion-button>
        </div>
      </transition>

      <!-- Indicateur de chargement -->
      <transition name="fade">
        <div v-if="loading" class="loading-overlay">
          <div class="loading-content">
            <ion-spinner name="crescent" color="primary"></ion-spinner>
            <span>Chargement des signalements...</span>
          </div>
        </div>
      </transition>
    </ion-content>

    <!-- Modal formulaire signalement amélioré -->
    <ion-modal :is-open="showForm" @didDismiss="closeForm" class="report-modal">
      <ion-header>
        <ion-toolbar class="modal-toolbar">
          <ion-buttons slot="start">
            <ion-button @click="closeForm" fill="clear" class="close-button">
              <ion-icon name="arrow-back-outline"></ion-icon>
            </ion-button>
          </ion-buttons>
          <ion-title>
            <div class="modal-title">
              <ion-icon name="create-outline"></ion-icon>
              <span>Nouveau signalement</span>
            </div>
          </ion-title>
        </ion-toolbar>
        
        <!-- Progress bar -->
        <div class="form-progress">
          <div class="progress-bar" :style="{ width: formProgress + '%' }"></div>
        </div>
      </ion-header>
      
      <ion-content class="modal-content">
        <div class="form-container">
          <!-- Position preview améliorée -->
          <div class="position-card">
            <div class="position-header">
              <div class="position-icon-wrapper">
                <ion-icon name="location" class="position-icon"></ion-icon>
                <span class="position-pin-pulse"></span>
              </div>
              <div class="position-info">
                <span class="position-label">Position sélectionnée</span>
                <span class="position-coords">
                  <span class="coord">{{ newPosition.lat.toFixed(5) }}°</span>
                  <span class="coord-separator">,</span>
                  <span class="coord">{{ newPosition.lng.toFixed(5) }}°</span>
                </span>
              </div>
            </div>
            <div class="position-hint">
              <ion-icon name="finger-print-outline"></ion-icon>
              <span>Cliquez sur la carte pour modifier</span>
            </div>
          </div>

          <!-- Formulaire principal -->
          <div class="form-card">
            <!-- Étape 1: Type de signalement -->
            <div class="form-section">
              <div class="section-header">
                <span class="section-number">1</span>
                <div class="section-info">
                  <h3 class="section-title">Type de signalement</h3>
                  <p class="section-subtitle">Sélectionnez le type de problème</p>
                </div>
              </div>
              
              <div class="type-grid">
                <div 
                  v-for="type in reportTypes" 
                  :key="type.value"
                  class="type-card"
                  :class="{ 'selected': reportType === type.value }"
                  @click="reportType = type.value"
                >
                  <div class="type-icon-wrapper" :class="type.class">
                    <ion-icon :name="type.icon"></ion-icon>
                  </div>
                  <span class="type-label">{{ type.label }}</span>
                  <ion-icon 
                    v-if="reportType === type.value" 
                    name="checkmark-circle" 
                    class="type-check"
                  ></ion-icon>
                </div>
              </div>
            </div>

            <!-- Étape 2: Description -->
            <div class="form-section">
              <div class="section-header">
                <span class="section-number">2</span>
                <div class="section-info">
                  <h3 class="section-title">Description</h3>
                  <p class="section-subtitle">Décrivez le problème rencontré</p>
                </div>
              </div>
              
              <div class="textarea-wrapper">
                <ion-textarea
                  v-model="reportDescription"
                  :placeholder="placeholderText"
                  :rows="5"
                  :maxlength="300"
                  class="description-textarea"
                ></ion-textarea>
                <div class="textarea-footer">
                  <span class="char-count" :class="{ 'warning': reportDescription.length > 250 }">
                    {{ reportDescription.length }}/300
                  </span>
                </div>
              </div>
            </div>

            <!-- Note d'information -->
            <div class="info-note">
              <div class="note-icon">
                <ion-icon name="shield-checkmark-outline"></ion-icon>
              </div>
              <div class="note-content">
                <span class="note-title">Validation requise</span>
                <span class="note-text">
                  Votre signalement sera vérifié par un modérateur avant publication
                </span>
              </div>
            </div>

            <!-- Boutons d'action -->
            <div class="action-buttons">
              <ion-button
                expand="block"
                @click="submitReport"
                :disabled="!canSubmit || submitting"
                class="submit-button"
              >
                <ion-spinner v-if="submitting" name="crescent" class="button-spinner"></ion-spinner>
                <span v-else>Créer le signalement</span>
              </ion-button>
              
              <ion-button
                expand="block"
                fill="clear"
                @click="closeForm"
                :disabled="submitting"
                class="cancel-button"
              >
                Annuler
              </ion-button>
            </div>
          </div>
        </div>
      </ion-content>
    </ion-modal>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import {
  IonPage, IonHeader, IonToolbar, IonTitle, IonContent, IonButtons, IonButton,
  IonIcon, IonFab, IonFabButton, IonModal, IonTextarea, IonSpinner, toastController
} from '@ionic/vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { db, auth } from '@/services/firebase';
import { collection, addDoc, getDocs, query, where } from 'firebase/firestore';
import { onAuthStateChanged } from 'firebase/auth';
import { logout } from '@/services/auth.service';
import { Geolocation } from '@capacitor/geolocation';

const router = useRouter();

// Refs
const mapContainer = ref<HTMLElement | null>(null);
const map = ref<L.Map | null>(null);
const markers: any[] = [];
const showForm = ref(false);
const reportType = ref('trou');
const reportDescription = ref('');
const newPosition = ref({ lat: -18.8792, lng: 47.5079 });
const isAuthenticated = ref(false);
const totalReports = ref(0);
const approvedReports = ref(0);
const pendingReports = ref(0);
const loading = ref(false);
const submitting = ref(false);
const showMyReports = ref(false);
const isTrackingLocation = ref(false);
const currentLocation = ref<L.LatLng | null>(null);
const userLocationMarker = ref<any>(null);
const watchId = ref<string | null>(null);
const isUserInteracting = ref(false);
const pendingLocationUpdate = ref<L.LatLng | null>(null);
const showLegend = ref(true);
const isLegendExpanded = ref(false);

// Computed
const formProgress = computed(() => {
  let progress = 0;
  if (reportType.value) progress += 50;
  if (reportDescription.value.trim().length >= 10) progress += 50;
  return progress;
});

const canSubmit = computed(() => {
  return reportType.value && reportDescription.value.trim().length >= 10;
});

const placeholderText = computed(() => {
  const placeholders: Record<string, string> = {
    trou: "Ex: Grand trou sur la route principale causant des dégâts aux véhicules...",
    chantier: "Ex: Travaux de réfection en cours, circulation alternée...",
    deviation: "Ex: Route fermée, déviation par la RN2..."
  };
  return placeholders[reportType.value] || "Décrivez le problème...";
});

const statsData = computed(() => [
  {
    label: 'Total',
    value: totalReports.value,
    icon: 'location-outline',
    class: 'primary'
  },
  {
    label: 'Validés',
    value: approvedReports.value,
    icon: 'checkmark-circle-outline',
    class: 'success'
  },
  {
    label: 'En attente',
    value: pendingReports.value,
    icon: 'time-outline',
    class: 'warning'
  }
]);

const reportTypes = [
  { value: 'trou', label: 'Trou', icon: 'warning-outline', class: 'danger' },
  { value: 'chantier', label: 'Chantier', icon: 'construct-outline', class: 'warning' },
  { value: 'deviation', label: 'Déviation', icon: 'swap-horizontal-outline', class: 'info' }
];

const legendItems = [
  { type: 'trou', label: 'Trou', icon: '!' },
  { type: 'chantier', label: 'Chantier', icon: '⚙' },
  { type: 'deviation', label: 'Déviation', icon: '↔' }
];

// Fonctions
const toggleLegend = () => {
  isLegendExpanded.value = !isLegendExpanded.value;
};

const toggleMyReports = () => {
  showMyReports.value = !showMyReports.value;
  loadReports();
};

const showToast = async (message: string, color: 'success' | 'warning' | 'danger' | 'primary' = 'success') => {
  const toast = await toastController.create({
    message,
    duration: 2000,
    position: 'top',
    color
  });
  await toast.present();
};

const startAddReport = () => {
  if (document.hidden) return;
  
  if (!isAuthenticated.value) {
    showToast('Veuillez vous connecter pour signaler un problème', 'warning');
    router.push('/login');
    return;
  }
  
  if (currentLocation.value) {
    newPosition.value = currentLocation.value;
  }
  
  reportType.value = 'trou';
  reportDescription.value = '';
  showForm.value = true;
};

const startAddReportAtPosition = (position: L.LatLng) => {
  if (!isAuthenticated.value) {
    showToast('Veuillez vous connecter pour signaler un problème', 'warning');
    router.push('/login');
    return;
  }
  
  newPosition.value = position;
  reportType.value = 'trou';
  reportDescription.value = '';
  showForm.value = true;
};

const closeForm = () => {
  showForm.value = false;
};

const zoomIn = () => {
  if (map.value) {
    map.value.zoomIn();
  }
};

const zoomOut = () => {
  if (map.value) {
    map.value.zoomOut();
  }
};

const applyLocationUpdate = (location: L.LatLng) => {
  if (!map.value || !isTrackingLocation.value) return;
  
  map.value.setView(location, map.value.getZoom(), {
    animate: true,
    duration: 0.5
  });
  
  if (userLocationMarker.value) {
    userLocationMarker.value.setLatLng(location);
  } else {
    const userIcon = L.divIcon({
      html: `
        <div class="user-location-marker">
          <div class="marker-core"></div>
          <div class="marker-pulse"></div>
        </div>
      `,
      className: '',
      iconSize: [40, 40],
      iconAnchor: [20, 20]
    });
    
    userLocationMarker.value = L.marker(location, { icon: userIcon });
    if (map.value) {
      map.value.addLayer(userLocationMarker.value);
    }
  }
};

const centerOnUser = async () => {
  if (document.hidden) return;

  if (isTrackingLocation.value) {
    stopLocationTracking();
    isTrackingLocation.value = false;
    showToast('Suivi GPS désactivé', 'primary');
    return;
  }

  try {
    const position = await Geolocation.getCurrentPosition({
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0
    });

    const coords = new L.LatLng(
      position.coords.latitude,
      position.coords.longitude
    );

    currentLocation.value = coords;
    applyLocationUpdate(coords);

    watchId.value = await Geolocation.watchPosition(
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      },
      (position, err) => {
        if (err || !position) return;
        
        const newCoords = new L.LatLng(
          position.coords.latitude,
          position.coords.longitude
        );
        
        currentLocation.value = newCoords;
        
        if (isUserInteracting.value) {
          pendingLocationUpdate.value = newCoords;
        } else {
          applyLocationUpdate(newCoords);
        }
      }
    );

    isTrackingLocation.value = true;
    showToast('Suivi GPS activé', 'success');
  } catch (error) {
    console.error('Erreur GPS:', error);
    showToast('Impossible d\'activer le GPS', 'danger');
  }
};

const stopLocationTracking = () => {
  if (watchId.value) {
    Geolocation.clearWatch({ id: watchId.value });
    watchId.value = null;
  }

  if (userLocationMarker.value && map.value) {
    map.value.removeLayer(userLocationMarker.value);
    userLocationMarker.value = null;
  }

  currentLocation.value = null;
};

const submitReport = async () => {
  if (document.hidden) return;

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

const goToLogin = () => {
  router.push('/login');
};

const recenterOnGPS = () => {
  if (currentLocation.value && map.value) {
    const wasInteracting = isUserInteracting.value;
    isUserInteracting.value = false;
    
    applyLocationUpdate(currentLocation.value);
    
    if (wasInteracting) {
      setTimeout(() => {
        isUserInteracting.value = true;
      }, 1000);
    }
    
    showToast('Carte recentrée', 'success');
  } else {
    showToast('Position GPS non disponible', 'warning');
  }
};

const getMarkerIcon = (type: string) => {
  const colors: Record<string, string> = {
    trou: '#ef4444',
    chantier: '#f59e0b',
    deviation: '#3b82f6'
  };

  const icons: Record<string, string> = {
    trou: '!',
    chantier: '⚙',
    deviation: '↔'
  };

  return L.divIcon({
    html: `
      <div class="custom-marker" style="--marker-color: ${colors[type] || '#64748b'}">
        <div class="marker-pin">
          <span class="marker-icon">${icons[type] || '?'}</span>
        </div>
        <div class="marker-shadow"></div>
      </div>
    `,
    className: '',
    iconSize: [40, 50],
    iconAnchor: [20, 50],
    popupAnchor: [0, -50]
  });
};

const loadReports = async () => {
  if (!map.value) return;

  loading.value = true;

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

    if (showMyReports.value && isAuthenticated.value && auth.currentUser) {
      q = query(reportsRef, where('uid', '==', auth.currentUser.uid));
    } else {
      q = reportsRef;
    }

    const snapshot = await getDocs(q);
    
    let approved = 0;
    let pending = 0;

    snapshot.forEach(doc => {
      const data: any = doc.data();
      
      if (data.status === 'approved') approved++;
      if (data.status === 'pending') pending++;

      if (data.status === 'approved' && map.value && data.lat && data.lng) {
        const marker = L.marker([data.lat, data.lng], {
          icon: getMarkerIcon(data.type)
        }).bindPopup(`
          <div class="custom-popup">
            <div class="popup-header">
              <span class="popup-type ${data.type}">${data.type}</span>
              <span class="popup-status">Validé</span>
            </div>
            <p class="popup-description">${data.description}</p>
            <div class="popup-footer">
              <ion-icon name="calendar-outline"></ion-icon>
              <span>${new Date(data.createdAt?.toDate()).toLocaleDateString('fr-FR')}</span>
            </div>
          </div>
        `, {
          className: 'custom-popup-container'
        });
        
        map.value.addLayer(marker);
        markers.push(marker);
      }
    });

    totalReports.value = snapshot.size;
    approvedReports.value = approved;
    pendingReports.value = pending;
  } catch (error) {
    console.error('Erreur chargement reports:', error);
    showToast('Erreur lors du chargement', 'danger');
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  onAuthStateChanged(auth, (user) => {
    isAuthenticated.value = !!user;
  });

  if (!mapContainer.value) return;

  await new Promise(resolve => setTimeout(resolve, 100));

  if (!mapContainer.value) return;

  try {
    map.value = L.map(mapContainer.value, {
      zoomControl: false,
      attributionControl: true
    }).setView([newPosition.value.lat, newPosition.value.lng], 13);

    const tileLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap',
      maxZoom: 19
    });
    map.value.addLayer(tileLayer);

    map.value.on('click', (e: L.LeafletMouseEvent) => {
      if (showForm.value) {
        newPosition.value = e.latlng;
        showToast('Position mise à jour', 'primary');
      } else {
        const confirmCreate = confirm(`Créer un signalement ici ?\n📍 ${e.latlng.lat.toFixed(5)}, ${e.latlng.lng.toFixed(5)}`);
        if (confirmCreate) {
          startAddReportAtPosition(e.latlng);
        }
      }
    });

    map.value.on('zoomstart movestart', () => {
      isUserInteracting.value = true;
    });

    map.value.on('zoomend moveend', () => {
      setTimeout(() => {
        isUserInteracting.value = false;
        
        if (pendingLocationUpdate.value) {
          applyLocationUpdate(pendingLocationUpdate.value);
          pendingLocationUpdate.value = null;
        }
      }, 150);
    });

    await loadReports();
  } catch (error) {
    console.error('Erreur initialisation carte:', error);
    showToast('Erreur lors du chargement de la carte', 'danger');
  }
});

const handleLogout = async () => {
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

onBeforeUnmount(() => {
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
@import '@/theme/map.css';
</style>