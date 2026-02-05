import { ref } from 'vue'
import { Capacitor } from '@capacitor/core'
import { PushNotifications } from '@capacitor/push-notifications'
import { doc, setDoc } from 'firebase/firestore'
import { db } from '@/services/firebase'

export function useNotifications() {
  const fcmToken = ref<string | null>(null)
  const permissionGranted = ref(false)
  const isInitialized = ref(false)

  /**
   * Initialise les notifications push (FCM)
   * Demande les permissions et enregistre le token
   */
  const initializePushNotifications = async () => {
    if (isInitialized.value) return

    try {
      // Vérifier que c'est une plateforme native
      if (!Capacitor.isNativePlatform()) {
        console.log('[Push] Non disponible sur web')
        return
      }

      // Demander la permission
      const permission = await PushNotifications.requestPermissions()
      console.log('[Push] Permission résultat:', permission)
      
      if (permission.receive === 'granted') {
        permissionGranted.value = true
        console.log('[Push] Permission accordée!')

        // Enregistrer le handler pour les notifications reçues
        await PushNotifications.addListener('pushNotificationReceived', (notification) => {
          console.log('[Push] Notification reçue:', notification)
        })

        // Handler pour quand l'utilisateur clique sur la notification
        await PushNotifications.addListener('pushNotificationActionPerformed', (notification) => {
          console.log('[Push] Action effectuée:', notification)
        })

        // Enregistrer l'appareil et récupérer le token
        console.log('[Push] Enregistrement de l\'appareil...')
        await PushNotifications.register()

        // Écouter le token
        await PushNotifications.addListener('registration', async (token) => {
          console.log('[FCM] Token reçu:', token.value)
          fcmToken.value = token.value
        })

        await PushNotifications.addListener('registrationError', (error) => {
          console.error('[FCM] Erreur enregistrement:', error)
        })
      } else {
        console.warn('[Push] Permission refusée')
      }

      isInitialized.value = true
    } catch (error) {
      console.error('[Push] Erreur initialisation:', error)
    }
  }

  /**
   * Sauvegarde le FCM token dans Firestore sous users/{uid}
   */
  const saveFcmTokenToFirebase = async (uid: string, token: string) => {
    try {
      console.log('[Firebase] Sauvegarde token pour uid:', uid)
      const userRef = doc(db, 'users', uid)
      await setDoc(
        userRef,
        {
          fcmToken: token,
          fcmTokenUpdatedAt: new Date()
        },
        { merge: true }
      )
      console.log('[Firebase] FCM token sauvegardé!')
    } catch (error) {
      console.error('[Firebase] Erreur sauvegarde FCM token:', error)
    }
  }

  /**
   * Met à jour le token quand l'utilisateur se connecte
   * Appelle init et sauvegarde le token une fois reçu
   */
  const updateTokenOnLogin = async (userId: string) => {
    if (fcmToken.value) {
      await saveFcmTokenToFirebase(userId, fcmToken.value)
    } else {
      // Si pas encore de token, initialiser d'abord
      console.log('[Notifications] Initialisation FCM pour userId:', userId)
      await initializePushNotifications()
      
      // Attendre un peu que le token soit reçu
      let attempts = 0
      while (!fcmToken.value && attempts < 30) {
        await new Promise(resolve => setTimeout(resolve, 100))
        attempts++
      }
      
      if (fcmToken.value) {
        await saveFcmTokenToFirebase(userId, fcmToken.value)
      } else {
        console.warn('[Notifications] Token FCM toujours pas reçu après 3 secondes')
      }
    }
  }

  return {
    fcmToken,
    permissionGranted,
    isInitialized,
    initializePushNotifications,
    updateTokenOnLogin
  }
}

