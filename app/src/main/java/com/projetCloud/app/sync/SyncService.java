package com.projetCloud.app.sync;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.projetCloud.app.config.ConnectivityService;
import com.projetCloud.app.utilisateurs.Utilisateur;
import com.projetCloud.app.utilisateurs.UtilisateurRepository;
import com.projetCloud.app.signalements.Signalement;
import com.projetCloud.app.signalements.SignalementService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import com.projetCloud.app.typesSignalement.TypeSignalement;
import com.projetCloud.app.typesSignalement.TypeSignalementService;

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

    @Autowired
    private SignalementService signalementService;

    @Autowired
    private TypeSignalementService typeSignalementService;

    /**
     * Synchronise les utilisateurs locaux non synchronisés vers Firebase
     * Gère aussi le cas où l'utilisateur existe déjà dans Firebase
     * @return nombre d'utilisateurs synchronisés
     * @throws TimeoutException 
     */
    public int syncUsersToFirebase() throws RuntimeException, TimeoutException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        List<Utilisateur> unsyncedUsers = utilisateurRepository.findByIsSyncedToFirebaseFalse();
        int syncedCount = 0;

        for (Utilisateur user : unsyncedUsers) {
            try {
                // Vérifier si l'utilisateur existe déjà dans Firebase par email
                UserRecord existingFirebaseUser = null;
                try {
                    existingFirebaseUser = FirebaseAuth.getInstance().getUserByEmail(user.getEmail());
                } catch (Exception e) {
                    // Utilisateur n'existe pas dans Firebase, continuer
                }

                if (existingFirebaseUser != null) {
                    // L'utilisateur existe déjà dans Firebase
                    // Utiliser son UID et le marquer comme synchronisé
                    System.out.println("L'utilisateur " + user.getEmail() + " existe déjà dans Firebase avec UID: " + existingFirebaseUser.getUid());
                    user.setFirebaseUid(existingFirebaseUser.getUid());
                    user.setIsSyncedToFirebase(true);
                    user.setLastSyncedAt(LocalDateTime.now());
                    user.setFirebaseCreatedAt(LocalDateTime.now());
                    user.setTempPassword(null); // Supprimer le mot de passe temporaire
                    utilisateurRepository.save(user);
                    syncedCount++;
                    continue;
                }

                // L'utilisateur n'existe pas dans Firebase, le créer
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
     * Gère aussi le cas où l'email existe déjà dans PostgreSQL sans firebase_uid
     * @return nombre d'utilisateurs synchronisés
     * @throws TimeoutException 
     */
    public int syncUsersFromFirebase() throws RuntimeException, TimeoutException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        try {
            // Récupérer tous les utilisateurs Firebase
            Iterable<com.google.firebase.auth.ExportedUserRecord> firebaseUsers = FirebaseAuth.getInstance().listUsers(null).iterateAll();

            int syncedCount = 0;
            for (com.google.firebase.auth.ExportedUserRecord firebaseUser : firebaseUsers) {
                // Vérifier si l'utilisateur existe déjà avec le firebase_uid
                Optional<Utilisateur> existingByUid = utilisateurRepository.findByFirebaseUid(firebaseUser.getUid());
                
                if (existingByUid.isPresent()) {
                    // Utilisateur déjà synchronisé, passer au suivant
                    continue;
                }
                
                // Vérifier si l'email existe déjà dans PostgreSQL (cas de création locale)
                Optional<Utilisateur> existingByEmail = utilisateurRepository.findByEmail(firebaseUser.getEmail());
                
                Utilisateur userToUpdate;
                if (existingByEmail.isPresent()) {
                    // L'utilisateur existe en local sans firebase_uid
                    // Mettre à jour avec le firebase_uid de Firebase
                    userToUpdate = existingByEmail.get();
                    System.out.println("Synchronisation de l'utilisateur existant " + firebaseUser.getEmail() + " avec firebase_uid: " + firebaseUser.getUid());
                } else {
                    // Créer un nouvel utilisateur
                    userToUpdate = new Utilisateur();
                    userToUpdate.setEmail(firebaseUser.getEmail());
                    System.out.println("Création du nouvel utilisateur " + firebaseUser.getEmail() + " depuis Firebase");
                }
                
                // Mettre à jour les champs
                userToUpdate.setFirebaseUid(firebaseUser.getUid());
                userToUpdate.setIsSyncedToFirebase(true);
                userToUpdate.setLastSyncedAt(LocalDateTime.now());
                
                // Mettre à jour les infos de profil seulement si pas déjà rempli
                if (userToUpdate.getNom() == null || userToUpdate.getNom().isEmpty()) {
                    userToUpdate.setNom(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName().split(" ")[0] : "Unknown");
                }
                if (userToUpdate.getPrenom() == null || userToUpdate.getPrenom().isEmpty()) {
                    userToUpdate.setPrenom(firebaseUser.getDisplayName() != null && firebaseUser.getDisplayName().split(" ").length > 1 ?
                                         firebaseUser.getDisplayName().split(" ")[1] : "");
                }
                
                // Définir source et statut si pas déjà défini
                if (userToUpdate.getIdSource() == null) {
                    userToUpdate.setIdSource(1); // Source Firebase
                }
                if (userToUpdate.getIdStatus() == null) {
                    userToUpdate.setIdStatus(1); // Actif par défaut
                }
                
                if (userToUpdate.getFirebaseCreatedAt() == null) {
                    userToUpdate.setFirebaseCreatedAt(firebaseUser.getUserMetadata() != null ?
                                                    LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(firebaseUser.getUserMetadata().getCreationTimestamp() / 1000),
                                                                           java.time.ZoneId.systemDefault()) : LocalDateTime.now());
                }

                utilisateurRepository.save(userToUpdate);
                syncedCount++;
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
        
        // Champs de sécurité pour le blocage de compte
        userData.put("is_blocked", false);
        userData.put("tentatives_connexion", 0);
        userData.put("last_failed_attempt", null);

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
     * @throws TimeoutException 
     */
    public Utilisateur createUserOnline(String email, String password, String nom, String prenom) throws RuntimeException, TimeoutException {
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
     * @throws TimeoutException 
     */
    public void updateUserOnline(Utilisateur user) throws RuntimeException, TimeoutException {
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
     * @throws TimeoutException 
     */
    public int syncOfflineModifications() throws RuntimeException, TimeoutException {
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

    /**
     * Ajoute les champs de sécurité manquants aux documents utilisateur existants
     * (is_blocked, tentatives_connexion, last_failed_attempt)
     */
    public int addSecurityFieldsToExistingUsers() throws RuntimeException {
        try {
            com.google.cloud.firestore.QuerySnapshot snapshot = firestore.collection("users").get().get();
            int updatedCount = 0;
            
            for (com.google.cloud.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    boolean needsUpdate = !data.containsKey("is_blocked") || 
                                        !data.containsKey("tentatives_connexion") || 
                                        !data.containsKey("last_failed_attempt");
                    
                    if (needsUpdate) {
                        Map<String, Object> updates = new HashMap<>();
                        if (!data.containsKey("is_blocked")) {
                            updates.put("is_blocked", false);
                        }
                        if (!data.containsKey("tentatives_connexion")) {
                            updates.put("tentatives_connexion", 0);
                        }
                        if (!data.containsKey("last_failed_attempt")) {
                            updates.put("last_failed_attempt", null);
                        }
                        
                        if (!updates.isEmpty()) {
                            firestore.collection("users").document(doc.getId()).update(updates).get();
                            updatedCount++;
                            System.out.println("Champs de securite ajoutes au document: " + doc.getId());
                        }
                    }
                }
            }
            
            return updatedCount;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise a jour des champs de securite: " + e.getMessage());
        }
    }

    /**
     * Synchronise l'état de blocage depuis Firebase (source de vérité) vers PostgreSQL
     * Récupère les champs: is_blocked, tentatives_connexion, last_failed_attempt
     * @return nombre d'utilisateurs synchronisés
     * @throws TimeoutException 
     */
    public int syncBlockStatusFromFirebase() throws TimeoutException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        int syncedCount = 0;
        
        try {
            // Récupérer tous les utilisateurs PostgreSQL
            List<Utilisateur> allUsers = utilisateurRepository.findAll();
            
            for (Utilisateur pgUser : allUsers) {
                try {
                    // Chercher le document Firestore correspondant (par email)
                    var firebaseUsers = firestore.collection("users")
                        .whereEqualTo("email", pgUser.getEmail())
                        .get()
                        .get()
                        .getDocuments();
                    
                    if (!firebaseUsers.isEmpty()) {
                        var fbDoc = firebaseUsers.get(0);
                        var fbData = fbDoc.getData();
                        
                        if (fbData != null) {
                            // Récupérer les données de blocage depuis Firebase
                            Boolean isBlocked = (Boolean) fbData.getOrDefault("is_blocked", false);
                            Long tentativesObj = (Long) fbData.get("tentatives_connexion");
                            Integer tentatives = tentativesObj != null ? tentativesObj.intValue() : 0;
                            // last_failed_attempt est un Timestamp Firebase, on l'ignore pour PostgreSQL
                            
                            // Vérifier si les données ont changé
                            boolean hasChanged = false;
                            if (pgUser.getIsBlocked() == null || !pgUser.getIsBlocked().equals(isBlocked)) {
                                pgUser.setIsBlocked(isBlocked);
                                hasChanged = true;
                            }
                            if (pgUser.getTentativesConnexion() == null || !pgUser.getTentativesConnexion().equals(tentatives)) {
                                pgUser.setTentativesConnexion(tentatives);
                                hasChanged = true;
                            }
                            
                            // Sauvegarder si les données ont changé
                            if (hasChanged) {
                                pgUser.setUpdatedAt(LocalDateTime.now());
                                utilisateurRepository.save(pgUser);
                                syncedCount++;
                                System.out.println("Blocage synchronisé pour l'utilisateur: " + pgUser.getEmail() 
                                    + " (blocked=" + isBlocked + ", tentatives=" + tentatives + ")");
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la synchronisation du blocage pour " + pgUser.getEmail() + ": " + e.getMessage());
                    // Continuer avec le prochain utilisateur
                }
            }
            
            return syncedCount;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la synchronisation du blocage depuis Firebase: " + e.getMessage());
        }
    }

    /**
     * Synchronise le déblocage d'un utilisateur vers Firebase
     * Appelle cette méthode après le déblocage dans PostgreSQL
     * @param utilisateurId L'ID PostgreSQL de l'utilisateur débloqué
     * @return true si la synchronisation a réussi
     * @throws TimeoutException 
     */
    public boolean syncDeblocageToFirebase(Long utilisateurId) throws TimeoutException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        try {
            // Récupérer l'utilisateur depuis PostgreSQL
            Optional<Utilisateur> userOpt = utilisateurRepository.findById(utilisateurId);
            
            if (!userOpt.isPresent()) {
                throw new RuntimeException("Utilisateur non trouvé dans PostgreSQL");
            }

            Utilisateur user = userOpt.get();
            
            // Chercher le document Firebase par email
            var firebaseUsers = firestore.collection("users")
                .whereEqualTo("email", user.getEmail())
                .get()
                .get()
                .getDocuments();
            
            if (firebaseUsers.isEmpty()) {
                System.out.println("Document Firebase non trouvé pour " + user.getEmail() + ", déblocage non synchronisé");
                return false;
            }

            // Mettre à jour le document Firebase avec l'état PostgreSQL
            var fbDoc = firebaseUsers.get(0);
            Map<String, Object> updates = new HashMap<>();
            updates.put("is_blocked", user.getIsBlocked() != null ? user.getIsBlocked() : false);
            updates.put("tentatives_connexion", user.getTentativesConnexion() != null ? user.getTentativesConnexion() : 0);
            updates.put("last_failed_attempt", null); // Réinitialiser après déblocage
            updates.put("updated_at", com.google.cloud.firestore.FieldValue.serverTimestamp());
            
            firestore.collection("users").document(fbDoc.getId()).update(updates).get();
            
            System.out.println("Déblocage synchronisé vers Firebase pour: " + user.getEmail());
            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation du déblocage vers Firebase: " + e.getMessage());
            throw new RuntimeException("Erreur synchronisation déblocage Firebase: " + e.getMessage());
        }
    }

    /**
     * Synchronise TOUS les déblocages depuis PostgreSQL vers Firebase
     * Envoie l'état de blocage de tous les utilisateurs
     * @return nombre d'utilisateurs synchronisés
     * @throws TimeoutException 
     */
    public int syncAllDeblocagesToFirebase() throws TimeoutException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        int syncedCount = 0;
        
        try {
            // Récupérer tous les utilisateurs PostgreSQL
            List<Utilisateur> allUsers = utilisateurRepository.findAll();
            
            for (Utilisateur user : allUsers) {
                try {
                    // Chercher le document Firestore correspondant (par email)
                    var firebaseUsers = firestore.collection("users")
                        .whereEqualTo("email", user.getEmail())
                        .get()
                        .get()
                        .getDocuments();
                    
                    if (!firebaseUsers.isEmpty()) {
                        var fbDoc = firebaseUsers.get(0);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("is_blocked", user.getIsBlocked() != null ? user.getIsBlocked() : false);
                        updates.put("tentatives_connexion", user.getTentativesConnexion() != null ? user.getTentativesConnexion() : 0);
                        updates.put("last_failed_attempt", null);
                        updates.put("updated_at", com.google.cloud.firestore.FieldValue.serverTimestamp());
                        
                        firestore.collection("users").document(fbDoc.getId()).update(updates).get();
                        syncedCount++;
                        System.out.println("Déblocage synchronisé pour: " + user.getEmail());
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la synchronisation du déblocage pour " + user.getEmail() + ": " + e.getMessage());
                    // Continuer avec le prochain utilisateur
                }
            }
            
            return syncedCount;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la synchronisation globale des déblocages: " + e.getMessage());
        }
    }

        /**
     * Synchronisation bi-directionnelle des signalements entre PostgreSQL et Firebase
     * Insère les signalements manquants dans chaque sens
     * @return nombre total de signalements synchronisés
     */
    public int syncSignalementsBidirectionnel() throws RuntimeException, TimeoutException {
        System.out.println("[SYNC][SIGNAL] Début synchronisation bi-directionnelle des signalements");
        int totalSynced = 0;
        try {
            if (!connectivityService.isFirebaseOnline()) {
                throw new RuntimeException("Firebase n'est pas accessible");
            }

            // 1. Récupérer tous les signalements de PostgreSQL
            List<Signalement> signalementsPg = signalementService.findAll();
            System.out.println("[SYNC][SIGNAL] Signalements PostgreSQL: " + signalementsPg.size());

            // 2. Récupérer tous les signalements de Firebase
            String firebaseCollection = "reports";
            List<QueryDocumentSnapshot> firebaseSignalements = firestore.collection(firebaseCollection).get().get().getDocuments();
            System.out.println("[SYNC][SIGNAL] Collection Firebase utilisée: " + firebaseCollection);
            System.out.println("[SYNC][SIGNAL] Signalements Firebase: " + firebaseSignalements.size());
            for (QueryDocumentSnapshot doc : firebaseSignalements) {
                System.out.println("[SYNC][SIGNAL][DEBUG] Doc id=" + doc.getId() + " data=" + doc.getData());
            }

            // 3. Indexer par ID
            java.util.Set<Long> idsPg = new java.util.HashSet<>();
            for (Signalement s : signalementsPg) idsPg.add(s.getId());
            java.util.Set<Long> idsFb = new java.util.HashSet<>();
            for (QueryDocumentSnapshot doc : firebaseSignalements) {
                try {
                    // On utilise l'ID du document comme identifiant unique côté Firebase
                    String docId = doc.getId();
                    // Si jamais un champ id numérique existe, on peut aussi l'ajouter
                    if (doc.contains("id")) {
                        Long id = doc.getLong("id");
                        if (id != null) idsFb.add(id);
                    }
                    // Ajoute aussi l'ID string pour la correspondance
                    try {
                        idsFb.add(Long.parseLong(docId));
                    } catch (NumberFormatException ignore) {}
                } catch (Exception e) {
                    System.err.println("[SYNC][SIGNAL] Erreur lecture id Firebase: " + e.getMessage());
                }
            }

            // 4. Insérer dans Firebase ceux présents en base mais absents dans Firebase
            for (Signalement s : signalementsPg) {
                if (!idsFb.contains(s.getId())) {
                    try {
                        Map<String, Object> data = new HashMap<>();
                        data.put("description", s.getDescription());
                        data.put("lat", s.getLatitude());
                        data.put("lng", s.getLongitude());
                        data.put("surfaceM2", s.getSurfaceM2());
                        data.put("status", s.getIdStatus());
                        data.put("type", s.getTypeSignalement() != null ? s.getTypeSignalement().getLibelle() : null);
                        data.put("uid", s.getUtilisateur() != null ? s.getUtilisateur().getFirebaseUid() : null);
                        data.put("createdAt", s.getCreatedAt() != null ? com.google.cloud.Timestamp.of(java.util.Date.from(s.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())) : null);
                        firestore.collection(firebaseCollection).document(String.valueOf(s.getId())).set(data).get();
                        System.out.println("[SYNC][SIGNAL] Ajouté dans Firebase: id=" + s.getId());
                        totalSynced++;
                    } catch (Exception e) {
                        System.err.println("[SYNC][SIGNAL] Erreur ajout Firebase id=" + s.getId() + " : " + e.getMessage());
                    }
                }
            }

            // 5. Insérer dans PostgreSQL ceux présents dans Firebase mais absents en base
            for (QueryDocumentSnapshot doc : firebaseSignalements) {
                String docId = doc.getId();
                Long id = null;
                // On tente d'utiliser l'ID numérique si possible, sinon l'ID string
                if (doc.contains("id")) {
                    id = doc.getLong("id");
                } else {
                    try {
                        id = Long.parseLong(docId);
                    } catch (NumberFormatException ignore) {}
                }
                if (id != null && !idsPg.contains(id)) {
                    try {
                        Signalement s = new Signalement();
                        s.setId(id);
                        s.setLatitude(doc.contains("lat") ? new java.math.BigDecimal(doc.get("lat").toString()) : null);
                        s.setLongitude(doc.contains("lng") ? new java.math.BigDecimal(doc.get("lng").toString()) : null);
                        s.setSurfaceM2(doc.contains("surfaceM2") ? new java.math.BigDecimal(doc.get("surfaceM2").toString()) : null);
                        s.setDescription(doc.contains("description") ? doc.getString("description") : null);
                        // Mapping type Firebase → id_type_signalement
                        if (doc.contains("type")) {
                            String libelleType = doc.getString("type");
                            if (libelleType != null) {
                                java.util.Optional<TypeSignalement> optType = typeSignalementService.findByLibelle(libelleType);
                                if (optType.isPresent()) {
                                    s.setTypeSignalement(optType.get());
                                } else {
                                    System.err.println("[SYNC][SIGNAL] TypeSignalement introuvable pour libelle: " + libelleType);
                                }
                            }
                        }
                        // TypeSignalement et Utilisateur à relier si besoin (optionnel)
                        s.setCreatedAt(doc.contains("createdAt") && doc.get("createdAt") != null ? ((com.google.cloud.Timestamp)doc.get("createdAt")).toSqlTimestamp().toLocalDateTime() : null);
                        signalementService.save(s);
                        System.out.println("[SYNC][SIGNAL] Ajouté dans PostgreSQL: id=" + id);
                        totalSynced++;
                    } catch (Exception e) {
                        System.err.println("[SYNC][SIGNAL] Erreur ajout PostgreSQL id=" + id + " : " + e.getMessage());
                    }
                }
            }

            System.out.println("[SYNC][SIGNAL] Synchronisation terminée. Total synchronisés: " + totalSynced);
        } catch (Exception e) {
            System.err.println("[SYNC][SIGNAL] Erreur générale: " + e.getMessage());
        }
        System.out.println("[SYNC][SIGNAL] Fin synchronisation bi-directionnelle des signalements");
        return totalSynced;
    }
}
