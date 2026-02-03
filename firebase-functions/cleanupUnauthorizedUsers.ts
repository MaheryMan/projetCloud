// Cloud Function pour supprimer les utilisateurs Firebase sans profil Firestore
// À déployer sur Firebase Cloud Functions

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

/**
 * Cloud Function déclenchée quand un document est créé dans Firebase Auth
 * Vérifie que le profil Firestore existe, sinon supprime l'utilisateur après 5 secondes
 */
export const cleanupUnauthorizedUsers = functions.auth.user().onCreate(async (user) => {
  const db = admin.firestore();
  const userDocRef = db.collection('users').doc(user.uid);
  
  // Attendre 5 secondes avant de vérifier
  setTimeout(async () => {
    const userDoc = await userDocRef.get();
    
    // Si le profil n'existe pas, supprimer l'utilisateur Firebase Auth
    if (!userDoc.exists) {
      try {
        await admin.auth().deleteUser(user.uid);
        console.log(`Utilisateur fantôme supprimé: ${user.email} (${user.uid})`);
      } catch (error) {
        console.error(`Erreur suppression utilisateur ${user.email}:`, error);
      }
    }
  }, 5000); // 5 secondes de délai
});
