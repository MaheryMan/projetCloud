import { auth, db } from './firebase'
import {
    signInWithEmailAndPassword,
    signOut,
    onAuthStateChanged,
    User,
    createUserWithEmailAndPassword,
    updateProfile,
    GoogleAuthProvider,
    signInWithCredential
} from 'firebase/auth'
import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth'
import {
    doc,
    getDoc,
    setDoc,
    serverTimestamp
} from 'firebase/firestore'
import type { UserProfile } from '@/types/user.types'

/**
 * Connexion email / mot de passe
 * (INSCRIPTION FAITE CÔTÉ WEB UNIQUEMENT)
 */
export async function login(email: string, password: string): Promise<User> {
    const result = await signInWithEmailAndPassword(auth, email, password)
    return result.user
}

/**
 * Inscription email / mot de passe
 */
export async function register(email: string, password: string, displayName?: string): Promise<User> {
    const cred = await createUserWithEmailAndPassword(auth, email, password)
    const user = cred.user

    if (displayName) {
        await updateProfile(user, { displayName })
    }

    const profile: UserProfile = {
        uid: user.uid,
        email: user.email,
        name: user.displayName || displayName || null,
        photoURL: user.photoURL,
        role: 'driver',
        createdAt: serverTimestamp() as any
    }

    const userRef = doc(db, 'users', user.uid)
    await setDoc(userRef, profile)

    return user
}

/**
 * Connexion via Google
 */
export async function loginWithGoogle(): Promise<User> {
    const googleUser = await GoogleAuth.signIn()

    // Créer un credential Firebase avec le token Google
    const credential = GoogleAuthProvider.credential(googleUser.authentication.idToken)
    const result = await signInWithCredential(auth, credential)
    const user = result.user

    // Création du profil Firestore si inexistant
    const userRef = doc(db, 'users', user.uid)
    const snap = await getDoc(userRef)

    if (!snap.exists()) {
        await setDoc(userRef, {
            uid: user.uid,
            email: user.email,
            name: user.displayName,
            photoURL: user.photoURL,
            provider: 'google',
            role: 'driver', // valeur par défaut
            createdAt: serverTimestamp()
        })
        console.log('🔥 Firestore user créé (Google)')
    }

    return user
}

/**
 * Déconnexion
 */
export async function logout(): Promise<void> {
    await signOut(auth)
}

/**
 * Écoute de la session utilisateur
 */
export function onAuthChange(callback: (user: User | null) => void) {
    return onAuthStateChanged(auth, callback)
}
