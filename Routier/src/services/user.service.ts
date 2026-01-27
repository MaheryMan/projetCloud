import { db } from './firebase'
import { doc, setDoc, getDoc } from 'firebase/firestore'
import type { UserProfile } from '@/types/user.types'

export async function createUserProfile(user: UserProfile) {
    const userRef = doc(db, 'users', user.uid)

    await setDoc(userRef, {
        email: user.email,
        name: user.name,
        photoURL: user.photoURL,
        role: user.role,
        createdAt: user.createdAt
    })
}

export async function getUserProfile(uid: string) {
    const userRef = doc(db, 'users', uid)
    const snapshot = await getDoc(userRef)

    return snapshot.exists() ? snapshot.data() : null
}
