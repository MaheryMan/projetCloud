import type { Id, Timestamp } from './common.types'

export type UserRole = 'driver' | 'admin'

export interface UserProfile {
  uid: Id
  email: string | null
  name: string | null
  photoURL: string | null
  role: UserRole
  createdAt: Timestamp
}
