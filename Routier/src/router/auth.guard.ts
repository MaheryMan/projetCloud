import { auth } from '@/services/firebase'

export function authGuard(to: any, from: any, next: any) {
  const user = auth.currentUser

  if (!user && to.path !== '/login') {
    next('/login')
  } else if (user && to.path === '/login') {
    next('/home')
  } else {
    next()
  }
}
