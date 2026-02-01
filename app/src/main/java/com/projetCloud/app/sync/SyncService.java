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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                user.setTempPassword(null); // Supprimer le mot de passe temporaire après sync
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
     * NOTE: Pour la sécurité, il faudrait :
     * - Soit stocker temporairement le mot de passe hashé et demander à l'utilisateur de le ressaisir lors du sync
     * - Soit utiliser un mécanisme de token temporaire
     * - Soit implémenter une logique de migration sécurisée
     */
    private String getPlainPasswordForUser(Utilisateur user) {
        // Utiliser le mot de passe temporaire stocké lors de la création offline
        if (user.getTempPassword() != null && !user.getTempPassword().isEmpty()) {
            return user.getTempPassword();
        }

        // TODO: Implémenter une logique sécurisée pour les autres cas
        // Pour l'instant, retourner null si pas de mot de passe temporaire
        return null;
    }

    /**
     * Synchronise les utilisateurs de Firebase vers PostgreSQL
     * Utile pour récupérer les comptes créés directement sur Firebase
     * @return nombre d'utilisateurs synchronisés
     */
    public int syncUsersFromFirebase() throws RuntimeException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        try {
            // Récupérer tous les utilisateurs Firebase
            Iterable<com.google.firebase.auth.ExportedUserRecord> firebaseUsers = FirebaseAuth.getInstance().listUsers(null).iterateAll();

            int syncedCount = 0;
            for (com.google.firebase.auth.ExportedUserRecord firebaseUser : firebaseUsers) {
                // Vérifier si l'utilisateur existe déjà dans PostgreSQL
                Optional<Utilisateur> existingUserOpt = utilisateurRepository.findByFirebaseUid(firebaseUser.getUid());
                if (!existingUserOpt.isPresent()) {
                    // Créer l'utilisateur dans PostgreSQL
                    Utilisateur newUser = new Utilisateur();
                    newUser.setEmail(firebaseUser.getEmail());
                    newUser.setFirebaseUid(firebaseUser.getUid());
                    newUser.setNom(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName().split(" ")[0] : "Unknown");
                    newUser.setPrenom(firebaseUser.getDisplayName() != null && firebaseUser.getDisplayName().split(" ").length > 1 ?
                                     firebaseUser.getDisplayName().split(" ")[1] : "Unknown");
                    newUser.setIdSource(1); // Source Firebase
                    newUser.setIdStatus(1); // Actif par défaut
                    newUser.setIsSyncedToFirebase(true);
                    newUser.setFirebaseCreatedAt(firebaseUser.getUserMetadata() != null ?
                                                LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(firebaseUser.getUserMetadata().getCreationTimestamp() / 1000),
                                                                       java.time.ZoneId.systemDefault()) : LocalDateTime.now());

                    utilisateurRepository.save(newUser);
                    syncedCount++;
                }
            }

            return syncedCount;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la sync depuis Firebase : " + e.getMessage());
        }
    }

    /**
     * Vérifie s'il y a des données à synchroniser
     * @return true si des données sont en attente de sync
     */
    public boolean hasPendingSync() {
        return !utilisateurRepository.findByIsSyncedToFirebaseFalse().isEmpty();
    }

    /**
     * Vérifie s'il y a des modifications hors ligne à synchroniser
     * @return true si des modifications sont en attente
     */
    public boolean hasOfflineModifications() {
        return !utilisateurRepository.findByModifiedOfflineTrue().isEmpty();
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

    /**
     * Crée un utilisateur en ligne : directement dans PostgreSQL et Firebase
     * @param email Email de l'utilisateur
     * @param password Mot de passe
     * @param nom Nom
     * @param prenom Prénom
     * @return Utilisateur créé
     */
    public Utilisateur createUserOnline(String email, String password, String nom, String prenom) throws RuntimeException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible pour la création en ligne");
        }

        UserRecord userRecord = null;
        try {
            // Créer dans Firebase Auth
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setDisplayName(nom + " " + prenom)
                    .setEmailVerified(false);

            userRecord = FirebaseAuth.getInstance().createUser(request);

            // Créer dans Firestore
            createFirestoreUserDocument(userRecord, nom, prenom, email);

            // Créer dans PostgreSQL
            Utilisateur user = new Utilisateur();
            user.setEmail(email);
            user.setNom(nom);
            user.setPrenom(prenom);
            user.setFirebaseUid(userRecord.getUid());
            user.setIsSyncedToFirebase(true);
            user.setLastSyncedAt(LocalDateTime.now());
            user.setFirebaseCreatedAt(LocalDateTime.now());
            user.setIdSource(1); // Source Firebase
            user.setIdStatus(1); // Actif par défaut

            return utilisateurRepository.save(user);
        } catch (Exception e) {
            // En cas d'erreur, supprimer l'utilisateur de Firebase s'il a été créé
            if (userRecord != null) {
                try {
                    FirebaseAuth.getInstance().deleteUser(userRecord.getUid());
                } catch (Exception deleteException) {
                    System.err.println("Erreur lors de la suppression de l'utilisateur Firebase : " + deleteException.getMessage());
                }
            }
            throw new RuntimeException("Erreur lors de la création en ligne : " + e.getMessage());
        }
    }

    /**
     * Authentifier un utilisateur hors ligne (pour l'app web React)
     * @param email Email
     * @param password Mot de passe
     * @return Utilisateur si authentifié, null sinon
     */
    public Utilisateur authenticateOffline(String email, String password) {
        Optional<Utilisateur> userOpt = utilisateurRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            Utilisateur user = userOpt.get();

            // Pour les comptes locaux (source = 3), vérifier le mot de passe hashé
            if (user.getIdSource() == 3 && user.getPassword() != null) {
                // TODO: Implémenter la vérification bcrypt du mot de passe
                // return passwordEncoder.matches(password, user.getPassword()) ? user : null;
                return user; // Temporaire
            }

            // Pour les comptes Firebase synchronisés, vérifier le mot de passe temporaire (si créé offline)
            if (user.getTempPassword() != null && user.getTempPassword().equals(password)) {
                return user;
            }

            // Pour les comptes Firebase déjà synchronisés, refuser l'authentification offline
            // (ils doivent utiliser Firebase Auth sur l'app mobile)
            if (user.getFirebaseUid() != null && user.getTempPassword() == null) {
                return null; // Pas d'authentification offline pour comptes Firebase
            }
        }

        return null;
    }

    /**
     * Générer un token temporaire pour l'authentification offline
     * @param user Utilisateur
     * @return Token JWT temporaire
     */
    public String generateOfflineToken(Utilisateur user) {
        // TODO: Implémenter génération JWT avec expiration courte
        // Pour l'instant, retourner un token simple
        return "offline_token_" + user.getId() + "_" + System.currentTimeMillis();
    }

    /**
     * Met à jour un utilisateur en ligne : synchronise vers Firebase
     * @param user Utilisateur à mettre à jour
     */
    public void updateUserOnline(Utilisateur user) throws RuntimeException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible pour la mise à jour");
        }

        try {
            // Mettre à jour Firebase Auth si nécessaire (ex. : email)
            if (user.getFirebaseUid() != null) {
                UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(user.getFirebaseUid())
                        .setEmail(user.getEmail())
                        .setDisplayName(user.getNom() + " " + user.getPrenom());

                FirebaseAuth.getInstance().updateUser(request);

                // Mettre à jour Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("email", user.getEmail());
                updates.put("name", user.getNom() + " " + user.getPrenom());
                updates.put("updatedAt", com.google.cloud.Timestamp.now());

                firestore.collection("users").document(user.getFirebaseUid()).update(updates).get();
            }

            // Sauvegarder dans PostgreSQL
            user.setLastSyncedAt(LocalDateTime.now());
            utilisateurRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour en ligne : " + e.getMessage());
        }
    }

    /**
     * Marque un utilisateur comme modifié hors ligne
     * @param user Utilisateur modifié
     */
    public void markUserModifiedOffline(Utilisateur user) {
        user.setModifiedOffline(true);
        user.setLastModifiedAt(LocalDateTime.now());
        utilisateurRepository.save(user);
    }

    /**
     * Synchronise les modifications hors ligne vers Firebase
     * @return nombre d'utilisateurs synchronisés
     */
    public int syncOfflineModifications() throws RuntimeException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        List<Utilisateur> modifiedUsers = utilisateurRepository.findByModifiedOfflineTrue();
        int syncedCount = 0;

        for (Utilisateur user : modifiedUsers) {
            try {
                updateUserOnline(user);
                user.setModifiedOffline(false);
                user.setLastSyncedAt(LocalDateTime.now());
                utilisateurRepository.save(user);
                syncedCount++;
            } catch (Exception e) {
                System.err.println("Erreur lors de la sync des modifications pour " + user.getEmail() + ": " + e.getMessage());
            }
        }

        return syncedCount;
    }
}
