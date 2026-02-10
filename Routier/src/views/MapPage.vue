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

      <div class="recap-toggle" @click="showRecap = !showRecap">
        <span class="recap-toggle-text">Récap</span>
        <span class="recap-toggle-state">{{ showRecap ? 'Masquer' : 'Afficher' }}</span>
      </div>

      <transition name="slide-fade">
        <ion-card v-if="showRecap" class="recap-card">
          <ion-card-content class="recap-content">
            <div class="recap-item">
              <span class="recap-label">Surface totale</span>
              <span class="recap-value">{{ totalSurfaceM2.toFixed(0) }} m²</span>
            </div>
            <div class="recap-item">
              <span class="recap-label">Budget total</span>
              <span class="recap-value">{{ totalBudgetEstimated.toFixed(0) }} Ar</span>
            </div>
            <div class="recap-item">
              <span class="recap-label">Avancement</span>
              <span class="recap-value">{{ progressPercent }}%</span>
            </div>
          </ion-card-content>
        </ion-card>
      </transition>

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

            <div class="form-section">
              <div class="section-header">
                <span class="section-number">3</span>
                <div class="section-info">
                  <h3 class="section-title">Détails</h3>
                  <p class="section-subtitle">Complétez les informations</p>
                </div>
              </div>

              <ion-item lines="none" class="input-item">
                <ion-input
                  v-model.number="surfaceM2"
                  label="Surface (m²)"
                  label-placement="floating"
                  inputmode="decimal"
                  type="number"
                />
              </ion-item>
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
                  v-for="type in availableTypes || []" 
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

            <!-- Étape 2.5: Photos -->
            <div class="form-section">
              <div class="section-header">
                <span class="section-number">2.5</span>
                <div class="section-info">
                  <h3 class="section-title">Photos (optionnel)</h3>
                  <p class="section-subtitle">Ajoutez jusqu'à 5 photos du problème</p>
                </div>
              </div>
              
              <PhotoPicker 
                ref="photoPickerRef"
                :disabled="submitting"
                @error="(msg) => showToast(msg, 'danger')"
              />
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

    <!-- Modal Photos Gallery -->
    <ion-modal
      :is-open="showPhotoGallery"
      @didDismiss="showPhotoGallery = false"
      class="photo-gallery-modal"
    >
      <ion-header>
        <ion-toolbar class="photo-gallery-toolbar">
          <ion-title>Photos du signalement</ion-title>
          <ion-buttons slot="end">
            <ion-button @click="showPhotoGallery = false" fill="clear">
              <ion-icon name="close-outline" slot="icon-only"></ion-icon>
            </ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>

      <ion-content class="photo-gallery-content" v-if="selectedReport">
        <div class="gallery-header">
          <div class="report-info">
            <span class="report-type">{{ selectedReport.type }}</span>
            <span class="report-status">{{ selectedReport.status }}</span>
          </div>
          <p class="report-desc">{{ selectedReport.description }}</p>
          <div class="report-extra-info" v-if="selectedReport.niveau !== null && selectedReport.niveau !== undefined">
            <ion-icon name="flag-outline"></ion-icon>
            <span>Niveau: {{ selectedReport.niveau }}</span>
          </div>
        </div>

        <PhotoGallery :report-id="selectedReport.id!" />
      </ion-content>
    </ion-modal>
  </ion-page>
</template>
@@
<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import {
  IonPage, IonHeader, IonToolbar, IonTitle, IonContent, IonButtons, IonButton,
  IonIcon, IonFabButton, IonModal, IonTextarea, IonSpinner, IonItem, IonInput,
  IonCard, IonCardContent
} from '@ionic/vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { logout } from '@/services/auth.service';
import { Geolocation } from '@capacitor/geolocation';
import { useToast } from '@/composables/useToast';
import { useAuth } from '@/composables/useAuth'
import { useReportMap } from '@/composables/useReportMap'
import { useReportForm } from '@/composables/useReportForm'
import PhotoPicker from '@/components/PhotoPicker.vue'
import PhotoGallery from '@/components/PhotoGallery.vue'
import type { ReportType } from '@/types/report.types'

const router = useRouter();

// Refs
const mapContainer = ref<HTMLElement | null>(null);
const showRecap = ref(false);
const newPosition = ref({ lat: -18.8792, lng: 47.5079 });
const showMyReports = ref(false);
const isTrackingLocation = ref(false);
const currentLocation = ref<L.LatLng | null>(null);
const userLocationMarker = ref<any>(null);
const watchId = ref<string | null>(null);
const isUserInteracting = ref(false);
const pendingLocationUpdate = ref<L.LatLng | null>(null);

const { showToast } = useToast();
const { user, isAuthenticated } = useAuth()
const userId = computed(() => user.value?.uid ?? null)

const reportForm = useReportForm({
  isAuthenticated,
  userId,
  newPosition,
  showToast,
  router,
  onCreated: () => reportMap.loadReports()
})

const reportMap = useReportMap({
  mapContainer,
  initialCenter: computed(() => newPosition.value),
  showForm: reportForm.showForm,
  newPosition,
  showMyReports,
  isAuthenticated,
  userId,
  showToast,
  onRequestCreateAtPosition: (position) => reportForm.startAddReportAtPosition({ lat: position.lat, lng: position.lng })
})

const {
  showForm,
  submitting,
  reportType,
  reportDescription,
  surfaceM2,
  photoPickerRef,
  formProgress,
  canSubmit,
  placeholderText,
  availableTypes,
  startAddReport: _startAddReport,
  closeForm,
  openPhotoOptions,
  submitReport
} = reportForm

const startAddReport = () => {
  if (currentLocation.value) {
    newPosition.value = currentLocation.value
  }
  _startAddReport()
}

const {
  loading,
  totalReports,
  approvedReports,
  pendingReports,
  inProgressReports,
  totalSurfaceM2,
  totalBudgetEstimated,
  progressPercent,
  showLegend,
  isLegendExpanded,
  selectedReport,
  showPhotoGallery,
  toggleLegend,
  zoomIn,
  zoomOut,
  toggleMyReports
} = reportMap

// Computed
const statsData = computed(() => [
  {
    label: 'Total',
    value: reportMap.totalReports.value,
    icon: 'location-outline',
    class: 'primary'
  },
  {
    label: 'Terminé',
    value: reportMap.approvedReports.value,
    icon: 'checkmark-circle-outline',
    class: 'success'
  },
  {
    label: 'Nouveau',
    value: reportMap.pendingReports.value,
    icon: 'time-outline',
    class: 'warning'
  },
  {
    label: 'En cours',
    value: reportMap.inProgressReports.value,
    icon: 'construct-outline',
    class: 'info'
  }
]);

// Les types sont maintenant fournis par le composable useReportForm (via metadata store)
// const reportTypes est défini dans le composable et exposé comme 'availableTypes'

const legendItems = [
  { type: 'nouveau', label: 'Nouveau', icon: '●' },
  { type: 'en_cours', label: 'En cours', icon: '●' },
  { type: 'termine', label: 'Terminé', icon: '●' }
];

// Fonctions

const applyLocationUpdate = (location: L.LatLng) => {
  if (!reportMap.map.value || !isTrackingLocation.value) return;
  
  reportMap.map.value.setView(location, reportMap.map.value.getZoom(), {
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
    reportMap.map.value.addLayer(userLocationMarker.value);
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

  if (userLocationMarker.value && reportMap.map.value) {
    reportMap.map.value.removeLayer(userLocationMarker.value);
    userLocationMarker.value = null;
  }

  currentLocation.value = null;
};

const goToLogin = () => {
  router.push('/login');
};

const recenterOnGPS = () => {
  if (currentLocation.value && reportMap.map.value) {
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

onMounted(async () => {
  try {
    await reportMap.initMap()

    reportMap.map.value?.on('zoomstart movestart', () => {
      isUserInteracting.value = true;
    });

    reportMap.map.value?.on('moveend', () => {
      setTimeout(() => {
        isUserInteracting.value = false;

        if (pendingLocationUpdate.value) {
          applyLocationUpdate(pendingLocationUpdate.value);
          pendingLocationUpdate.value = null;
        }
      }, 150);
    });

    reportMap.map.value?.on('zoomend', () => {
      setTimeout(() => {
        isUserInteracting.value = false;
        // Ne pas appliquer les mises à jour GPS en attente après un zoom
        // pour éviter que la carte ne se recentre automatiquement
        pendingLocationUpdate.value = null;
      }, 150);
    });
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

  reportMap.destroyMap()
});
</script>

<style scoped>
@import '@/theme/map.css';
</style>