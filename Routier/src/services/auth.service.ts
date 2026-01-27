import { auth, db } from './firebase'
import {
    signInWithEmailAndPassword,
    signOut,
    onAuthStateChanged,
    User,
    GoogleAuthProvider,
    signInWithPopup
} from 'firebase/auth'
import {
    doc,
    getDoc,
    setDoc,
    serverTimestamp
} from 'firebase/firestore'

/**
 * Connexion email / mot de passe
 * (INSCRIPTION FAITE CÔTÉ WEB UNIQUEMENT)
 */
export async function login(email: string, password: string): Promise<User> {
    const result = await signInWithEmailAndPassword(auth, email, password)
    return result.user
}

/**
 * Connexion via Google
 */
export async function loginWithGoogle(): Promise<User> {
    const provider = new GoogleAuthProvider()
    const result = await signInWithPopup(auth, provider)
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
