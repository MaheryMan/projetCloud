import { db } from './firebase'
import { doc, getDoc, updateDoc, serverTimestamp } from 'firebase/firestore'

export interface SecurityConfig {
  tentatives_max: number
  reset_after_success: boolean
  message_blocked: string
}

export interface UserSecurityStatus {
  is_blocked: boolean
  tentatives_connexion: number
  last_failed_attempt: any
}

/**
 * Récupère la configuration de sécurité depuis Firebase
 */
export async function getSecurityConfig(): Promise<SecurityConfig> {
  try {
    const configRef = doc(db, 'config', 'security')
    const snap = await getDoc(configRef)

    if (!snap.exists()) {
      // Configuration par défaut si elle n'existe pas
      return {
        tentatives_max: 3,
        reset_after_success: true,
        message_blocked: 'Compte bloqué. Contactez un manager.'
      }
    }

    return snap.data() as SecurityConfig
  } catch (error) {
    console.error('Erreur lors de la récupération de la config de sécurité:', error)
    // Retourner la configuration par défaut en cas d'erreur
    return {
      tentatives_max: 3,
      reset_after_success: true,
      message_blocked: 'Compte bloqué. Contactez un manager.'
    }
  }
}

/**
 * Récupère le statut de sécurité de l'utilisateur
 */
export async function getUserSecurityStatus(uid: string): Promise<UserSecurityStatus> {
  try {
    const userRef = doc(db, 'users', uid)
    const snap = await getDoc(userRef)

    if (!snap.exists()) {
      return {
        is_blocked: false,
        tentatives_connexion: 0,
        last_failed_attempt: null
      }
    }

    const data = snap.data()
    return {
      is_blocked: data.is_blocked || false,
      tentatives_connexion: data.tentatives_connexion || 0,
      last_failed_attempt: data.last_failed_attempt || null
    }
  } catch (error) {
    console.error('Erreur lors de la récupération du statut de sécurité:', error)
    return {
      is_blocked: false,
      tentatives_connexion: 0,
      last_failed_attempt: null
    }
  }
}

/**
 * Vérifie si l'utilisateur est bloqué
 */
export async function isUserBlocked(uid: string): Promise<boolean> {
  const status = await getUserSecurityStatus(uid)
  return status.is_blocked
}

/**
 * Incrémente le nombre de tentatives échouées
 * Bloque le compte si le nombre max est atteint
 */
export async function incrementFailedAttempts(uid: string): Promise<{
  blocked: boolean
  attempts: number
  maxAttempts: number
  message: string
}> {
  try {
    const config = await getSecurityConfig()
    const status = await getUserSecurityStatus(uid)

    // Incrémenter les tentatives
    const newAttempts = (status.tentatives_connexion || 0) + 1
    const isNowBlocked = newAttempts >= config.tentatives_max

    // Mettre à jour le document utilisateur
    const userRef = doc(db, 'users', uid)
    await updateDoc(userRef, {
      tentatives_connexion: newAttempts,
      is_blocked: isNowBlocked,
      last_failed_attempt: serverTimestamp()
    })

    // Retourner les informations
    if (isNowBlocked) {
      return {
        blocked: true,
        attempts: newAttempts,
        maxAttempts: config.tentatives_max,
        message: config.message_blocked
      }
    } else {
      return {
        blocked: false,
        attempts: newAttempts,
        maxAttempts: config.tentatives_max,
        message: `Mot de passe incorrect (${newAttempts}/${config.tentatives_max})`
      }
    }
  } catch (error) {
    console.error('Erreur lors de l\'incrémentation des tentatives:', error)
    throw error
  }
}

/**
 * Réinitialise les tentatives après une connexion réussie
 */
export async function resetFailedAttempts(uid: string): Promise<void> {
  try {
    const config = await getSecurityConfig()

    if (config.reset_after_success) {
      const userRef = doc(db, 'users', uid)
      await updateDoc(userRef, {
        tentatives_connexion: 0,
        last_failed_attempt: null
      })
    }
  } catch (error) {
    console.error('Erreur lors de la réinitialisation des tentatives:', error)
    // Ne pas lever l'erreur, car ce n'est pas critique
  }
}

/**
 * Obtient le message d'erreur descriptif basé sur le code d'erreur Firebase
 */
export function getLoginErrorMessage(errorCode: string, attempts: number = 0, maxAttempts: number = 3): string {
  switch (errorCode) {
    case 'auth/user-not-found':
      return 'Email non trouvé'
    case 'auth/wrong-password':
      return `Mot de passe incorrect (${attempts}/${maxAttempts})`
    case 'auth/invalid-email':
      return 'Email invalide'
    case 'auth/user-disabled':
      return 'Ce compte a été désactivé'
    case 'auth/too-many-requests':
      return 'Trop de tentatives. Veuillez réessayer plus tard.'
    default:
      return 'Erreur de connexion'
  }
}
