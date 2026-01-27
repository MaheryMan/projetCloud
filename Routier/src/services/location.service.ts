/**
 * Service pour récupérer la localisation GPS
 */

export interface Location {
  latitude: number
  longitude: number
}

export type LocationCallback = (location: Location) => void

// État du suivi GPS
let watchId: number | null = null
let isWatching = false

/**
 * Récupère la position GPS actuelle
 */
export async function getCurrentLocation(): Promise<Location> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('La géolocalisation n\'est pas supportée par ce navigateur'))
      return
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude
        })
      },
      (error) => {
        let message = 'Erreur lors de la récupération de la localisation'
        switch (error.code) {
          case error.PERMISSION_DENIED:
            message = 'Permission de géolocalisation refusée'
            break
          case error.POSITION_UNAVAILABLE:
            message = 'Position indisponible'
            break
          case error.TIMEOUT:
            message = 'Timeout lors de la récupération de la position'
            break
        }
        reject(new Error(message))
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 300000 // 5 minutes
      }
    )
  })
}

/**
 * Démarre le suivi continu de la position GPS
 */
export function startLocationTracking(callback: LocationCallback): void {
  if (!navigator.geolocation) {
    throw new Error('La géolocalisation n\'est pas supportée par ce navigateur')
  }

  if (isWatching) {
    stopLocationTracking()
  }

  watchId = navigator.geolocation.watchPosition(
    (position) => {
      const location: Location = {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude
      }
      callback(location)
    },
    (error) => {
      console.error('Erreur de suivi GPS:', error)
      // En cas d'erreur, on arrête le suivi
      stopLocationTracking()
    },
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 30000 // 30 secondes
    }
  )

  isWatching = true
}

/**
 * Arrête le suivi de la position GPS
 */
export function stopLocationTracking(): void {
  if (watchId !== null) {
    navigator.geolocation.clearWatch(watchId)
    watchId = null
  }
  isWatching = false
}

/**
 * Vérifie si le suivi GPS est actif
 */
export function isLocationTrackingActive(): boolean {
  return isWatching
}