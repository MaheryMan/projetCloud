import { auth, db } from './firebase'
import {
    signInWithEmailAndPassword,
    signOut,
    onAuthStateChanged,
    User,
    createUserWithEmailAndPassword,
    updateProfile,
    GoogleAuthProvider,
    signInWithCredential,
    signInWithPopup,
    deleteUser,
    AuthError
} from 'firebase/auth'
import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth'
import { Capacitor } from '@capacitor/core'
import {
    doc,
    getDoc,
    setDoc,
    serverTimestamp,
    query,
    collection,
    where,
    getDocs
} from 'firebase/firestore'
import type { UserProfile } from '@/types/user.types'
import { isUserBlocked, incrementFailedAttempts, resetFailedAttempts, getSecurityConfig } from './account-security.service'

/**
 * Connexion email / mot de passe
 * (INSCRIPTION FAITE CÔTÉ WEB UNIQUEMENT)
 * 
 * Gère le système de tentatives et blocage de compte
 */
export async function login(email: string, password: string): Promise<User> {
    try {
        // 1. Chercher l'utilisateur par email
        const usersRef = collection(db, 'users')
        const q = query(usersRef, where('email', '==', email))
        const querySnapshot = await getDocs(q)

        if (querySnapshot.empty) {
            // Utilisateur non trouvé
            throw new Error('Email non trouvé')
        }

        const userDoc = querySnapshot.docs[0]
        const userUID = userDoc.id
        const userData = userDoc.data()

        // 2. Vérifier si le compte est bloqué
        if (userData.is_blocked === true) {
            const config = await getSecurityConfig()
            throw new Error(config.message_blocked)
        }

        // 3. Tenter la connexion Firebase
        let firebaseUser: User
        try {
            const result = await signInWithEmailAndPassword(auth, email, password)
            firebaseUser = result.user
        } catch (firebaseError: any) {
            // 4. Si mot de passe incorrect, incrémenter les tentatives
            // Firebase retourne auth/invalid-credential ou auth/wrong-password
            if (firebaseError.code === 'auth/wrong-password' || firebaseError.code === 'auth/invalid-credential') {
                const failureResult = await incrementFailedAttempts(userUID)
                throw new Error(failureResult.message)
            }
            throw firebaseError
        }

        // 5. Connexion réussie : réinitialiser les tentatives
        await resetFailedAttempts(userUID)

        return firebaseUser
    } catch (error: any) {
        console.error('Erreur lors de la connexion:', error)
        throw error
    }
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
    let user: User

    if (Capacitor.isNativePlatform()) {
        const googleUser = await GoogleAuth.signIn()

        // Créer un credential Firebase avec le token Google
        const credential = GoogleAuthProvider.credential(googleUser.authentication.idToken)
        const result = await signInWithCredential(auth, credential)
        user = result.user
    } else {
        const provider = new GoogleAuthProvider()
        provider.setCustomParameters({ prompt: 'select_account' })
        const result = await signInWithPopup(auth, provider)
        user = result.user
    }

    // Vérifier si le profil Firestore existe (doit avoir été créé par le web)
    const userRef = doc(db, 'users', user.uid)
    const snap = await getDoc(userRef)

    if (!snap.exists()) {
        // Profil inexistant : nettoyer
        try {
            // 1. D'abord se déconnecter
            await signOut(auth)
            
            // 2. Puis supprimer l'utilisateur de Firebase Auth
            await deleteUser(user)
        } catch (e) {
            console.error('Erreur nettoyage compte Firebase:', e)
            // Continuer même si la suppression échoue
        }
        
        // 3. Lever l'erreur
        throw new Error('Compte non autorisé. Veuillez contacter l\'administrateur pour créer votre compte.')
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
