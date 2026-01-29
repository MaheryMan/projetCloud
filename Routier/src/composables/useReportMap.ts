import { ref, type Ref } from 'vue'
import L from 'leaflet'
import { getAllReports, getReportsByUser } from '@/services/report.service'
import { computeReportMetrics } from '@/utils/reportMetrics'
import type { Report } from '@/types/report.types'

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
    const statusColors: Record<string, string> = {
      nouveau: '#3b82f6',
      en_cours: '#f59e0b',
      termine: '#22c55e'
    }

    const icons: Record<string, string> = {
      trou: '!',
      chantier: '⚙',
      deviation: '↔'
    }

    return L.divIcon({
      html: `
        <div class="custom-marker" style="--marker-color: ${statusColors[status] || '#64748b'}">
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
    const approved = reports.filter((r) => r.status === 'termine').length
    const pending = reports.filter((r) => r.status === 'nouveau').length
    const inProgress = reports.filter((r) => r.status === 'en_cours').length

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

    const marker = L.marker([report.lat, report.lng], {
      icon: getMarkerIcon(report.type, report.status)
    }).bindPopup(
      `
        <div class="custom-popup">
          <div class="popup-header">
            <span class="popup-type ${report.type}">${report.type}</span>
            <span class="popup-status">${report.status}</span>
          </div>
          <p class="popup-description">${report.description}</p>
          ${report.photo ? `<div class="popup-photo"><img src="${report.photo}" alt="Photo du signalement" /></div>` : ''}
          <div class="popup-details">
            <div class="popup-detail"><strong>Surface:</strong> ${(report.surfaceM2 || 0)} m²</div>
            <div class="popup-detail"><strong>Budget estimé:</strong> ${(report.budgetEstimated || 0)} Ar</div>
            <div class="popup-detail"><strong>Entreprise:</strong> ${report.companyName || 'Non spécifiée'}</div>
          </div>
          <div class="popup-footer">
            <ion-icon name="calendar-outline"></ion-icon>
            <span>${new Date(report.createdAt as any).toLocaleDateString('fr-FR')}</span>
          </div>
        </div>
      `,
      {
        className: 'custom-popup-container'
      }
    )

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
    toggleLegend,
    zoomIn,
    zoomOut,
    toggleMyReports,
    loadReports,
    initMap,
    destroyMap
  }
}
