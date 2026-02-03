import { ref } from 'vue'
import { useToast } from './useToast'

export function useErrorHandler() {
  const lastError = ref<string | null>(null)
  const { showToast } = useToast()

  const handleError = (error: unknown, fallbackMessage = 'Une erreur est survenue') => {
    const message = (error as any)?.message || fallbackMessage
    console.error(error)
    lastError.value = message
    showToast(message, 'danger')
  }

  const clearError = () => {
    lastError.value = null
  }

  return {
    lastError,
    handleError,
    clearError
  }
}
