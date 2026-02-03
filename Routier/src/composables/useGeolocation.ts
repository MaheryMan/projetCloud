import { ref, onMounted, onBeforeUnmount } from 'vue'
import { getCurrentLocation, startLocationTracking, stopLocationTracking, type Location } from '@/services/location.service'

export function useGeolocation() {
  const currentLocation = ref<Location | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const tracking = ref(false)

  const fetchCurrentLocation = async () => {
    loading.value = true
    error.value = null
    try {
      const loc = await getCurrentLocation()
      currentLocation.value = loc
      return loc
    } catch (e: any) {
      error.value = e?.message || 'Erreur de géolocalisation'
      throw e
    } finally {
      loading.value = false
    }
  }

  const startTracking = () => {
    try {
      startLocationTracking((loc) => {
        currentLocation.value = loc
      })
      tracking.value = true
    } catch (e: any) {
      error.value = e?.message || 'Erreur lors du démarrage du suivi'
      tracking.value = false
    }
  }

  const stopTracking = () => {
    stopLocationTracking()
    tracking.value = false
  }

  onMounted(() => {
    // Optionnel: récupérer la position au montage
    fetchCurrentLocation().catch(() => {})
  })

  onBeforeUnmount(() => {
    if (tracking.value) {
      stopTracking()
    }
  })

  return {
    currentLocation,
    loading,
    error,
    tracking,
    fetchCurrentLocation,
    startTracking,
    stopTracking
  }
}
