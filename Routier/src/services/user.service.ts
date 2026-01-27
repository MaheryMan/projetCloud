import { db } from './firebase'
import { doc, setDoc, getDoc } from 'firebase/firestore'

export interface UserProfile {
    uid: string
    email: string
    createdAt: Date
}

export async function createUserProfile(user: UserProfile) {
    const userRef = doc(db, 'users', user.uid)

    await setDoc(userRef, {
        email: user.email,
        createdAt: user.createdAt
    })
}

export async function getUserProfile(uid: string) {
    const userRef = doc(db, 'users', uid)
    const snapshot = await getDoc(userRef)

    return snapshot.exists() ? snapshot.data() : null
}
