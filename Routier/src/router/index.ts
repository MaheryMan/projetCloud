import { createRouter, createWebHistory } from '@ionic/vue-router'
import { RouteRecordRaw } from 'vue-router'
import { auth } from '@/services/firebase'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginPage.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/MapPage.vue'),
    meta: { requiresAuth: false } // Changé à false pour permettre l'accès sans connexion
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// État global pour l'authentification
let authReady = false
let currentUser: any = null

// Attendre que Firebase détermine l'état d'authentification
import { onAuthStateChanged } from 'firebase/auth'
onAuthStateChanged(auth, (user) => {
  currentUser = user
  authReady = true
})

/**
 * Auth Guard Firebase - attend que l'auth soit prête
 */
router.beforeEach(async (to, from, next) => {
  // Attendre que Firebase ait déterminé l'état d'authentification
  if (!authReady) {
    await new Promise(resolve => {
      const checkAuth = () => {
        if (authReady) {
          resolve(void 0)
        } else {
          setTimeout(checkAuth, 50)
        }
      }
      checkAuth()
    })
  }

  const requiresAuth = to.meta.requiresAuth

  if (requiresAuth && !currentUser) {
    next('/login')
  } else if (to.path === '/login' && currentUser) {
    next('/home')
  } else {
    next()
  }
})

export default router
