package com.projetCloud.app.sync;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.projetCloud.app.config.ConnectivityService;
import com.projetCloud.app.utilisateurs.Utilisateur;
import com.projetCloud.app.utilisateurs.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service pour synchroniser les données entre PostgreSQL et Firebase
 */
@Service
public class SyncService {

    @Autowired
    private ConnectivityService connectivityService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private Firestore firestore;

    /**
     * Synchronise les utilisateurs locaux non synchronisés vers Firebase
     * @return nombre d'utilisateurs synchronisés
     */
    public int syncUsersToFirebase() throws RuntimeException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        List<Utilisateur> unsyncedUsers = utilisateurRepository.findByIsSyncedToFirebaseFalse();
        int syncedCount = 0;

        for (Utilisateur user : unsyncedUsers) {
            try {
                // Vérifier si on a le mot de passe en clair (stocké temporairement)
                String plainPassword = getPlainPasswordForUser(user);
                if (plainPassword == null || plainPassword.isEmpty()) {
                    System.err.println("Mot de passe manquant pour la sync de l'utilisateur " + user.getEmail());
                    continue;
                }

                // Créer l'utilisateur dans Firebase Auth
                UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                        .setEmail(user.getEmail())
                        .setPassword(plainPassword)
                        .setDisplayName(user.getNom() + " " + user.getPrenom())
                        .setEmailVerified(false);

                UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);

                // Créer le document Firestore
                createFirestoreUserDocument(userRecord, user.getNom(), user.getPrenom(), user.getEmail());

                // Mettre à jour l'utilisateur local
                user.setFirebaseUid(userRecord.getUid());
                user.setIsSyncedToFirebase(true);
                user.setLastSyncedAt(LocalDateTime.now());
                user.setFirebaseCreatedAt(LocalDateTime.now());
                utilisateurRepository.save(user);

                syncedCount++;
            } catch (Exception e) {
                // Log l'erreur mais continue avec les autres utilisateurs
                System.err.println("Erreur lors de la sync de l'utilisateur " + user.getEmail() + ": " + e.getMessage());
            }
        }

        return syncedCount;
    }

    /**
     * Récupère le mot de passe en clair pour un utilisateur
     * Pour l'instant, on utilise une logique temporaire (à améliorer)
     */
    private String getPlainPasswordForUser(Utilisateur user) {
        // TODO: Implémenter une logique sécurisée pour stocker/récupérer le mot de passe en clair
        // Pour l'instant, on suppose que les utilisateurs créés offline ont leur mot de passe
        // stocké temporairement (cette logique devra être revue pour la sécurité)
        if (user.getIdSource() == 3) { // Utilisateur local
            // Ici on devrait avoir une logique pour récupérer le mot de passe en clair
            // Par exemple depuis un champ temporaire ou en demandant à l'utilisateur
            return null; // Temporairement null
        }
        return null;
    }

    /**
     * Vérifie s'il y a des données à synchroniser
     * @return true si des données sont en attente de sync
     */
    public boolean hasPendingSync() {
        return !utilisateurRepository.findByIsSyncedToFirebaseFalse().isEmpty();
    }

    /**
     * Crée un document Firestore pour l'utilisateur
     */
    private void createFirestoreUserDocument(UserRecord userRecord, String nom, String prenom, String email) throws Exception {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", userRecord.getUid());
        userData.put("email", email);
        userData.put("name", nom + " " + prenom);
        userData.put("provider", "email");
        userData.put("role", "driver");
        userData.put("createdAt", com.google.cloud.Timestamp.now());

        if (userRecord.getPhotoUrl() != null) {
            userData.put("photoURL", userRecord.getPhotoUrl());
        }

        DocumentReference docRef = firestore.collection("users").document(userRecord.getUid());
        docRef.set(userData).get();
    }
}
