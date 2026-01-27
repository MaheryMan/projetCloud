/**
 * Service pour récupérer la localisation GPS
 */

import { Geolocation } from '@capacitor/geolocation'

export interface Location {
  latitude: number
  longitude: number
}

export type LocationCallback = (location: Location) => void

// État du suivi GPS
let watchId: string | null = null
let isWatching = false

/**
 * Récupère la position GPS actuelle
 */
export async function getCurrentLocation(): Promise<Location> {
  // Demander les permissions si nécessaire
  const permissions = await Geolocation.requestPermissions()
  if (permissions.location !== 'granted') {
    throw new Error('Permission de géolocalisation refusée')
  }

  const position = await Geolocation.getCurrentPosition({
    enableHighAccuracy: true,
    timeout: 10000,
    maximumAge: 300000 // 5 minutes
  })

  return {
    latitude: position.coords.latitude,
    longitude: position.coords.longitude
  }
}

/**
 * Démarre le suivi continu de la position GPS
 */
export async function startLocationTracking(callback: LocationCallback): Promise<void> {
  // Demander les permissions si nécessaire
  const permissions = await Geolocation.requestPermissions()
  if (permissions.location !== 'granted') {
    throw new Error('Permission de géolocalisation refusée')
  }

  if (isWatching) {
    stopLocationTracking()
  }

  const id = await Geolocation.watchPosition(
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 30000 // 30 secondes
    },
    (position, err) => {
      if (err) {
        console.error('Erreur de suivi GPS:', err)
        stopLocationTracking()
        return
      }
      if (position) {
        const location: Location = {
          latitude: position.coords.latitude,
          longitude: position.coords.longitude
        }
        callback(location)
      }
    }
  )

  watchId = id
  isWatching = true
}

/**
 * Arrête le suivi de la position GPS
 */
export function stopLocationTracking(): void {
  if (watchId !== null) {
    Geolocation.clearWatch({ id: watchId })
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