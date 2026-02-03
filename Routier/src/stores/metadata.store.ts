import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { fetchAllMetadata, type TypeSignalement, type Status } from '@/services/metadata.service'

export const useMetadataStore = defineStore('metadata', () => {
  const types = ref<TypeSignalement[]>([])
  const statuses = ref<Status[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const initialized = ref(false)

  const isLoaded = computed(() => initialized.value && types.value.length > 0 && statuses.value.length > 0)

  /**
   * Charge les métadonnées depuis Firebase
   */
  const loadMetadata = async () => {
    if (initialized.value) {
      // Déjà chargé, ne pas recharger
      return
    }

    loading.value = true
    error.value = null

    try {
      const data = await fetchAllMetadata()
      types.value = data.types
      statuses.value = data.statuses
      initialized.value = true
    } catch (e: any) {
      error.value = e?.message || 'Erreur lors du chargement des métadonnées'
      console.error('Erreur metadata:', error.value)
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * Réinitialise le store (par exemple à la déconnexion)
   */
  const reset = () => {
    types.value = []
    statuses.value = []
    loading.value = false
    error.value = null
    initialized.value = false
  }

  /**
   * Retourne un type par son ID
   */
  const getTypeById = (id: string): TypeSignalement | undefined => {
    return types.value.find((t) => t.id === id)
  }

  /**
   * Retourne un type par son code
   */
  const getTypeByCode = (code: string): TypeSignalement | undefined => {
    return types.value.find((t) => t.code === code)
  }

  /**
   * Retourne un statut par son ID
   */
  const getStatusById = (id: string): Status | undefined => {
    return statuses.value.find((s) => s.id === id)
  }

  /**
   * Retourne un statut par son code
   */
  const getStatusByCode = (code: string): Status | undefined => {
    return statuses.value.find((s) => s.code === code)
  }

  return {
    // State
    types,
    statuses,
    loading,
    error,
    initialized,

    // Computed
    isLoaded,

    // Actions
    loadMetadata,
    reset,
    getTypeById,
    getTypeByCode,
    getStatusById,
    getStatusByCode
  }
}, {
  persist: true
})
