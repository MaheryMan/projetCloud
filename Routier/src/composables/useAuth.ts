import { ref, onMounted, onUnmounted, readonly, computed } from 'vue'
import { User, onAuthStateChanged } from 'firebase/auth'
import { auth } from '@/services/firebase'

const user = ref<User | null>(null)
const isAuthReady = ref(false)

let unsubscribe: (() => void) | null = null

export function useAuth() {
  onMounted(() => {
    if (!unsubscribe) {
      unsubscribe = onAuthStateChanged(auth, (firebaseUser) => {
        user.value = firebaseUser
        isAuthReady.value = true
      })
    }
  })

  onUnmounted(() => {
    if (unsubscribe) {
      unsubscribe()
      unsubscribe = null
    }
  })

  return {
    user: readonly(user),
    isAuthReady: readonly(isAuthReady),
    isAuthenticated: computed(() => !!user.value)
  }
}