import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from 'firebase/auth'
import { onAuthChange, login, loginWithGoogle, logout } from '@/services/auth.service'
import { useMetadataStore } from './metadata.store'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const initialized = ref(false)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Initialisation de l'écoute Firebase
  if (!initialized.value) {
    onAuthChange((firebaseUser) => {
      user.value = firebaseUser
      initialized.value = true
    })
  }

  const isAuthenticated = computed(() => !!user.value)

  const loginWithEmail = async (email: string, password: string) => {
    loading.value = true
    error.value = null
    try {
      const u = await login(email, password)
      user.value = u
      // Charger les métadonnées après login
      const metadataStore = useMetadataStore()
      await metadataStore.loadMetadata()
    } catch (e: any) {
      error.value = e?.message || 'Erreur de connexion'
      throw e
    } finally {
      loading.value = false
    }
  }

  const loginGoogleAction = async () => {
    loading.value = true
    error.value = null
    try {
      const u = await loginWithGoogle()
      user.value = u
      // Charger les métadonnées après login
      const metadataStore = useMetadataStore()
      await metadataStore.loadMetadata()
    } catch (e: any) {
      error.value = e?.message || 'Erreur de connexion Google'
      throw e
    } finally {
      loading.value = false
    }
  }

  const logoutAction = async () => {
    await logout()
    user.value = null
    // Réinitialiser les métadonnées à la déconnexion
    const metadataStore = useMetadataStore()
    metadataStore.reset()
  }

  return {
    // state
    user,
    initialized,
    loading,
    error,
    // getters
    isAuthenticated,
    // actions
    loginWithEmail,
    loginGoogleAction,
    logoutAction
  }
})
