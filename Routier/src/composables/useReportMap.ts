import { ref, type Ref } from 'vue'
import L from 'leaflet'
import { getAllReports, getReportsByUser } from '@/services/report.service'
import { computeReportMetrics, getStatusProgress } from '@/utils/reportMetrics'
import type { Report } from '@/types/report.types'
import { useMetadataStore } from '@/stores/metadata.store'
import { normalizeString, compareNormalized, findNormalized } from '@/utils/stringNormalization'

type LatLng = L.LatLng

type ToastColor = 'primary' | 'success' | 'warning' | 'danger' | 'medium'
type ShowToast = (message: string, color?: ToastColor, duration?: number) => Promise<void> | void

export interface UseReportMapOptions {
  mapContainer: Ref<HTMLElement | null>
  initialCenter: Ref<{ lat: number; lng: number }>
  showForm: Ref<boolean>
  newPosition: Ref<{ lat: number; lng: number }>
  showMyReports: Ref<boolean>
  isAuthenticated: Ref<boolean>
  userId: Ref<string | null>
  showToast: ShowToast
  onRequestCreateAtPosition: (position: LatLng) => void
}

export function useReportMap(options: UseReportMapOptions) {
  const map = ref<L.Map | null>(null)
  const markers: L.Marker[] = []
  const metadataStore = useMetadataStore()
  const selectedReport = ref<Report | null>(null)
  const showPhotoGallery = ref(false)

  const totalReports = ref(0)
  const approvedReports = ref(0)
  const pendingReports = ref(0)
  const inProgressReports = ref(0)
  const totalSurfaceM2 = ref(0)
  const totalBudgetEstimated = ref(0)
  const progressPercent = ref(0)
  const loading = ref(false)

  const showLegend = ref(true)
  const isLegendExpanded = ref(false)

  const toggleLegend = () => {
    isLegendExpanded.value = !isLegendExpanded.value
  }

  const zoomIn = () => {
    map.value?.zoomIn()
  }

  const zoomOut = () => {
    map.value?.zoomOut()
  }

  const getMarkerIcon = (type: string, status: string) => {
    // Couleurs mappées par statut normalisé
    const statusColors: Record<string, string> = {
      'nouveau': '#3b82f6',      // Bleu
      'en_cours': '#f59e0b',     // Orange
      'termine': '#22c55e'       // Vert
    }

    // Normaliser le statut pour le matching
    const normalizedStatus = normalizeString(status)
    
    // Chercher la couleur du statut par matching normalisé
    let statusColor = '#64748b' // Couleur par défaut (gris)
    
    // Vérifier directement par code normalisé
    for (const [code, color] of Object.entries(statusColors)) {
      if (compareNormalized(normalizedStatus, code)) {
        statusColor = color
        break
      }
    }
    
    // Si pas trouvé, chercher en matchant avec les libellés metadata
    if (statusColor === '#64748b') {
      const matchingStatus = findNormalized(metadataStore.statuses, status, (s) => s.libelle)
      if (matchingStatus && matchingStatus.code) {
        for (const [code, color] of Object.entries(statusColors)) {
          if (compareNormalized(matchingStatus.code, code)) {
            statusColor = color
            break
          }
        }
      }
    }

    // Trouver l'icône et la couleur du type depuis la metadata
    let typeIcon = '?'
    let typeColor = '#64748b'
    
    // Chercher par libellé (nouveau style)
    const matchingType = findNormalized(metadataStore.types, type, (t) => t.libelle)
    if (matchingType) {
      // Utiliser la première lettre du libellé comme icône ou la couleur si disponible
      typeIcon = matchingType.libelle.charAt(0).toUpperCase()
      typeColor = matchingType.couleur || typeColor
    } else {
      // Fallback sur codes (ancien style) - mapping personnalisé
      const iconMapping: Record<string, string> = {
        'trou': '!',
        'chantier': '⚙',
        'deviation': '↔',
        'autre': '?'
      }
      typeIcon = iconMapping[normalizeString(type)] || '?'
    }


    return L.divIcon({
      html: `
        <div class="custom-marker" style="--marker-color: ${statusColor}; --type-color: ${typeColor}">
          <div class="marker-pin">
            <span class="marker-icon">${typeIcon}</span>
          </div>
          <div class="marker-shadow"></div>
        </div>
      `,
      className: '',
      iconSize: [40, 50],
      iconAnchor: [20, 50],
      popupAnchor: [0, -50]
    })
  }

  const clearMarkers = () => {
    markers.forEach((m) => {
      try {
        map.value?.removeLayer(m)
      } catch (e) {
        console.warn('Erreur suppression marker:', e)
      }
    })
    markers.length = 0
  }

  const setReportStatsFromReports = (reports: Report[]) => {
    // Chercher les libellés des statuts depuis la metadata
    const getStatusLibelles = () => {
      const libelles: Record<string, string> = {}
      metadataStore.statuses.forEach((s) => {
        if (s.code) libelles[s.code] = s.libelle
      })
      return libelles
    }

    const statusLibelles = getStatusLibelles()
    
    // Compter par code d'abord, puis par libellé pour compatibilité
    const terminéStatus = statusLibelles['termine'] || 'Terminé'
    const nouveauStatus = statusLibelles['nouveau'] || 'Nouveau'
    const enCoursStatus = statusLibelles['en_cours'] || 'En cours'

    const approved = reports.filter((r) => 
      r.status === 'termine' || r.status === terminéStatus
    ).length
    const pending = reports.filter((r) => 
      r.status === 'nouveau' || r.status === nouveauStatus
    ).length
    const inProgress = reports.filter((r) => 
      r.status === 'en_cours' || r.status === enCoursStatus
    ).length

    totalReports.value = reports.length
    approvedReports.value = approved
    pendingReports.value = pending
    inProgressReports.value = inProgress

    const metrics = computeReportMetrics(reports)
    totalSurfaceM2.value = metrics.totalSurfaceM2
    totalBudgetEstimated.value = metrics.totalBudgetEstimated
    progressPercent.value = metrics.progressPercent
  }

  const addReportMarker = (report: Report) => {
    if (!map.value) return

    const progress = getStatusProgress(report.status)
    const progressColor = progress === 100 ? '#22c55e' : progress === 50 ? '#f59e0b' : '#3b82f6'

    const marker = L.marker([report.lat, report.lng], {
      icon: getMarkerIcon(report.type, report.status)
    }).bindPopup(
      `
        <div class="custom-popup">
          <div class="popup-header">
            <span class="popup-type ${report.type}">${report.type}</span>
            <span class="popup-status ${report.status.toLowerCase().replace(/\s+/g, '_')}">${report.status}</span>
          </div>
          <p class="popup-description">${report.description}</p>
          <div class="popup-progress-section">
            <div class="popup-progress-header">
              <span class="popup-progress-label">Avancement</span>
              <span class="popup-progress-value" style="color: ${progressColor}">${progress}%</span>
            </div>
            <div class="popup-progress-bar">
              <div class="popup-progress-fill" style="width: ${progress}%; background: ${progressColor}"></div>
            </div>
          </div>
          <div class="popup-details">
            <div class="popup-detail"><strong>Surface:</strong> ${(report.surfaceM2 || 0)} m²</div>
            <div class="popup-detail"><strong>Budget estimé:</strong> ${(report.budgetEstimated || 0)} Ar</div>
            <div class="popup-detail"><strong>Entreprise:</strong> ${report.companyName || 'Non spécifiée'}</div>
            <div class="popup-detail"><strong>Niveau:</strong> ${report.niveau !== null && report.niveau !== undefined ? report.niveau : 'Non défini'}</div>
          </div>
          <div class="popup-footer">
            <ion-icon name="calendar-outline"></ion-icon>
            <span>${new Date(report.createdAt as any).toLocaleDateString('fr-FR')}</span>
          </div>
          <div class="popup-actions">
            <button class="popup-photo-btn" id="btn-photos-${report.id}">
              <ion-icon name="images-outline"></ion-icon> Voir les photos
            </button>
          </div>
        </div>
      `,
      {
        className: 'custom-popup-container'
      }
    )

    marker.on('popupopen', () => {
      const btn = document.getElementById(`btn-photos-${report.id}`)
      if (btn) {
        btn.addEventListener('click', () => {
          selectedReport.value = report
          showPhotoGallery.value = true
        })
      }
    })

    map.value.addLayer(marker)
    markers.push(marker)
  }

  const loadReports = async () => {
    if (!map.value) return

    loading.value = true
    clearMarkers()

    try {
      let reports: Report[]

      if (options.showMyReports.value && options.isAuthenticated.value && options.userId.value) {
        reports = await getReportsByUser(options.userId.value)
      } else {
        reports = await getAllReports()
      }

      reports.forEach((report) => {
        if (report.lat && report.lng) {
          addReportMarker(report)
        }
      })

      setReportStatsFromReports(reports)
    } catch (error) {
      console.error('Erreur chargement reports:', error)
      options.showToast('Erreur lors du chargement', 'danger')
    } finally {
      loading.value = false
    }
  }

  const toggleMyReports = () => {
    options.showMyReports.value = !options.showMyReports.value
    loadReports()
  }

  const initMap = async () => {
    if (!options.mapContainer.value) return

    await new Promise((resolve) => setTimeout(resolve, 100))

    if (!options.mapContainer.value) return

    map.value = L.map(options.mapContainer.value, {
      zoomControl: false,
      attributionControl: true
    }).setView([options.initialCenter.value.lat, options.initialCenter.value.lng], 13)

    const tileLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap',
      maxZoom: 19
    })

    map.value.addLayer(tileLayer)

    map.value.on('click', (e: L.LeafletMouseEvent) => {
      if (options.showForm.value) {
        options.newPosition.value = { lat: e.latlng.lat, lng: e.latlng.lng }
        options.showToast('Position mise à jour', 'primary')
      } else {
        const confirmCreate = confirm(
          `Créer un signalement ici ?\n📍 ${e.latlng.lat.toFixed(5)}, ${e.latlng.lng.toFixed(5)}`
        )
        if (confirmCreate) {
          options.onRequestCreateAtPosition(e.latlng)
        }
      }
    })

    await loadReports()
  }

  const destroyMap = () => {
    clearMarkers()
    if (map.value) {
      map.value.remove()
      map.value = null
    }
  }

  return {
    map,
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
    toggleMyReports,
    loadReports,
    initMap,
    destroyMap
  }
}
