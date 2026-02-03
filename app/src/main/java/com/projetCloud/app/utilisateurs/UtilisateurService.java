package com.projetCloud.app.utilisateurs;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.projetCloud.app.config.ConnectivityService;
import com.projetCloud.app.roles.Role;
import com.projetCloud.app.roles.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service pour gérer les utilisateurs
 */
@Service
public class UtilisateurService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ConnectivityService connectivityService;

    @Autowired
    private Firestore firestore;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Authentifie un utilisateur
     *
     * @param email    l'email de l'utilisateur
     * @param password le mot de passe de l'utilisateur
     * @return l'utilisateur authentifié si les informations sont correctes, sinon un Optional vide
     */
    public Optional<Utilisateur> authenticate(String email, String password) {
        Optional<Utilisateur> utilisateur = utilisateurRepository.findByEmail(email);
        if (utilisateur.isPresent()
                && utilisateur.get().getDeletedAt() == null) {
            String storedPassword = utilisateur.get().getPassword();
            boolean passwordMatches = false;
            if (storedPassword != null) {
                if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2y$") || storedPassword.startsWith("$2b$")) {
                    // Password is BCrypt hashed
                    passwordMatches = passwordEncoder.matches(password, storedPassword);
                } else {
                    // Password is plain text (for backward compatibility)
                    passwordMatches = password.equals(storedPassword);
                }
            }
            if (passwordMatches) {
                return utilisateur;
            }
        }
        return Optional.empty();
    }

    /**
     * Recherche un utilisateur par son email
     *
     * @param email l'email de l'utilisateur
     * @return l'utilisateur si trouvé, sinon un Optional vide
     */
    public Optional<Utilisateur> findByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .filter(u -> u.getDeletedAt() == null);
    }

    /**
     * Recherche un utilisateur par son id
     *
     * @param id l'id de l'utilisateur
     * @return l'utilisateur si trouvé, sinon un Optional vide
     */
    public Optional<Utilisateur> findByUid(Long id) {
        return utilisateurRepository.findById(id);
    }

    /**
     * Recherche un utilisateur par son id
     *
     * @param id l'id de l'utilisateur
     * @return l'utilisateur si trouvé, sinon un Optional vide
     */
    public Optional<Utilisateur> findById(Long id) {
        return utilisateurRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null);
    }

    /**
     * Récupère la liste de tous les utilisateurs
     *
     * @return la liste des utilisateurs
     */
    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll().stream()
                .filter(u -> u.getDeletedAt() == null)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les rôles d'un utilisateur
     *
     * @param utilisateurId l'id de l'utilisateur
     * @return la liste des rôles de l'utilisateur
     */
    public List<String> getUserRoles(Long utilisateurId) {
        return utilisateurRepository.findRoleLibellesByUtilisateurId(utilisateurId);
    }

    /**
     * Vérifie si un utilisateur est manager
     *
     * @param utilisateurId l'id de l'utilisateur
     * @return true si l'utilisateur est manager, false sinon
     */
    public boolean isManager(Long utilisateurId) {
        List<String> roles = getUserRoles(utilisateurId);
        return roles.contains("Manager");
    }

    /**
     * Inscrit un nouvel utilisateur
     * Si online, crée dans Firebase Auth et localement
     * Si offline, crée seulement localement
     *
     * @param email email de l'utilisateur
     * @param password mot de passe
     * @param nom nom
     * @param prenom prénom
     * @param numTel numéro de téléphone (optionnel)
     * @return l'utilisateur créé
     * @throws Exception si l'inscription échoue
     */
    public Utilisateur register(String email, String password, String nom, String prenom, String numTel) throws Exception {
        // Vérifier si l'email existe déjà
        if (utilisateurRepository.findByEmail(email).isPresent()) {
            throw new Exception("Un utilisateur avec cet email existe déjà");
        }

        boolean isOnline = connectivityService.isFirebaseOnline();
        String firebaseUid = null;
        LocalDateTime firebaseCreatedAt = null;

        if (isOnline) {
            try {
                // Créer l'utilisateur dans Firebase Auth
                UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                        .setEmail(email)
                        .setPassword(password)
                        .setDisplayName(nom + " " + prenom)
                        .setEmailVerified(false);

                UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
                firebaseUid = userRecord.getUid();
                firebaseCreatedAt = LocalDateTime.now();

                // Créer le document Firestore
                createFirestoreUserDocument(userRecord, nom, prenom, email);

            } catch (FirebaseAuthException e) {
                throw new Exception("Erreur lors de la création dans Firebase: " + e.getMessage());
            } catch (Exception e) {
                throw new Exception("Erreur lors de la création du document Firestore: " + e.getMessage());
            }
        }

        // Créer l'utilisateur localement
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setPassword(password); // Toujours hasher le mot de passe localement
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setNumTel(numTel);
        utilisateur.setFirebaseUid(firebaseUid);
        utilisateur.setIdSource(isOnline ? 1 : 3); // 1 = firebase_email, 3 = local
        utilisateur.setIdStatus(1); // Actif
        utilisateur.setIsSyncedToFirebase(isOnline);
        utilisateur.setFirebaseCreatedAt(firebaseCreatedAt);
        utilisateur.setCreatedAt(LocalDateTime.now());

        if (!isOnline) {
            utilisateur.setTempPassword(password); // Stocker en clair pour la sync future
        } else {
            utilisateur.setTempPassword(null); // Pas de temp pour online, déjà sync
        }

        Utilisateur savedUser = save(utilisateur);

        // Assigner le rôle Mobile_User par défaut
        Optional<Role> mobileUserRole = roleRepository.findByLibelle("Mobile_User");
        if (mobileUserRole.isPresent()) {
            savedUser.getRoles().add(mobileUserRole.get());
            utilisateurRepository.save(savedUser);
        }

        return savedUser;
    }

    /**
     * Inscrit un nouvel utilisateur via Google OAuth
     * Vérifie le token Google, crée dans Firebase si nécessaire, puis dans PostgreSQL
     *
     * @param idToken token Google ID
     * @param nom nom (optionnel, pris de Google)
     * @param prenom prénom (optionnel, pris de Google)
     * @param numTel numéro de téléphone (optionnel)
     * @return l'utilisateur créé
     * @throws Exception si l'inscription échoue
     */
    public Utilisateur registerWithGoogle(String idToken, String nom, String prenom, String numTel) throws Exception {
        if (!connectivityService.isFirebaseOnline()) {
            throw new Exception("Firebase n'est pas accessible pour l'authentification Google");
        }

        // Vérifier le token Google
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        String uid = decodedToken.getUid();
        String email = decodedToken.getEmail();
        String name = decodedToken.getName();
        String picture = decodedToken.getPicture();

        // Vérifier si l'utilisateur existe déjà dans PostgreSQL
        Optional<Utilisateur> existingUser = utilisateurRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new Exception("Un utilisateur avec cet email existe déjà");
        }

        // Vérifier si l'utilisateur existe dans Firebase (devrait exister via Google)
        UserRecord userRecord;
        try {
            userRecord = FirebaseAuth.getInstance().getUser(uid);
        } catch (FirebaseAuthException e) {
            throw new Exception("Utilisateur Google non trouvé dans Firebase: " + e.getMessage());
        }

        // Créer le document Firestore si inexistant
        createFirestoreUserDocumentForGoogle(userRecord, name, email, picture);

        // Extraire nom et prénom de name si non fournis
        if (nom == null || nom.isEmpty()) {
            String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};
            nom = nameParts.length > 0 ? nameParts[0] : "";
            prenom = nameParts.length > 1 ? nameParts[1] : "";
        }

        // Créer l'utilisateur dans PostgreSQL
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setNumTel(numTel);
        utilisateur.setFirebaseUid(uid);
        utilisateur.setIdSource(2); // Google OAuth
        utilisateur.setIdStatus(1); // Actif
        utilisateur.setIsSyncedToFirebase(true);
        utilisateur.setFirebaseCreatedAt(userRecord.getUserMetadata() != null ? 
            LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(userRecord.getUserMetadata().getCreationTimestamp()), 
                java.time.ZoneId.systemDefault()) : LocalDateTime.now());
        utilisateur.setCreatedAt(LocalDateTime.now());

        Utilisateur savedUser = save(utilisateur);

        // Assigner le rôle Mobile_User par défaut
        Optional<Role> mobileUserRole = roleRepository.findByLibelle("Mobile_User");
        if (mobileUserRole.isPresent()) {
            savedUser.getRoles().add(mobileUserRole.get());
            utilisateurRepository.save(savedUser);
        }

        return savedUser;
    }

    /**
     * Enregistre un utilisateur
     *
     * @param utilisateur l'utilisateur à enregistrer
     * @return l'utilisateur enregistré
     */
    public Utilisateur save(Utilisateur utilisateur) {
        if (utilisateur.getPassword() != null && !utilisateur.getPassword().isEmpty()) {
            String pwd = utilisateur.getPassword();
            boolean alreadyBcrypt = pwd.startsWith("$2a$") || pwd.startsWith("$2y$") || pwd.startsWith("$2b$");
            if (!alreadyBcrypt) {
                utilisateur.setPassword(passwordEncoder.encode(pwd));
            }
        }
        return utilisateurRepository.save(utilisateur);
    }

    /**
     * Supprime un utilisateur
     *
     * @param id l'id de l'utilisateur à supprimer
     */
    public void deleteById(Long id) {
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(id);
        if (utilisateurOpt.isPresent()) {
            Utilisateur utilisateur = utilisateurOpt.get();
            utilisateur.setDeletedAt(LocalDateTime.now());
            utilisateurRepository.save(utilisateur);
        }
    }

    /**
     * Crée un document Firestore pour l'utilisateur Google
     */
    private void createFirestoreUserDocumentForGoogle(UserRecord userRecord, String name, String email, String picture) {
        try {
            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", userRecord.getUid());
            userData.put("email", email);
            userData.put("name", name);
            userData.put("photoURL", picture);
            userData.put("provider", "google");
            userData.put("role", "driver"); // valeur par défaut
            userData.put("createdAt", com.google.cloud.Timestamp.now());
            
            // Champs de sécurité pour le blocage de compte
            userData.put("is_blocked", false);
            userData.put("tentatives_connexion", 0);
            userData.put("last_failed_attempt", null);

            DocumentReference docRef = firestore.collection("users").document(userRecord.getUid());
            docRef.set(userData); // Asynchrone, pas de .get()
            System.out.println("Firestore document creation initiated for " + email);
        } catch (Exception e) {
            System.out.println("Erreur lors de la création Firestore: " + e.getMessage());
            // Ne pas throw, pour ne pas bloquer la création DB
        }
    }

    /**
     * Crée un document Firestore pour l'utilisateur
     */
    private void createFirestoreUserDocument(UserRecord userRecord, String nom, String prenom, String email) throws Exception {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", userRecord.getUid());
        userData.put("email", email);
        userData.put("name", nom + " " + prenom);
        userData.put("provider", "email"); // ou "google" selon le cas
        userData.put("role", "driver"); // rôle par défaut, peut être modifié
        userData.put("createdAt", com.google.cloud.Timestamp.now());
        
        // Champs de sécurité pour le blocage de compte
        userData.put("is_blocked", false);
        userData.put("tentatives_connexion", 0);
        userData.put("last_failed_attempt", null);

        // Ajouter photoURL si disponible (null pour l'instant)
        if (userRecord.getPhotoUrl() != null) {
            userData.put("photoURL", userRecord.getPhotoUrl());
        }

        DocumentReference docRef = firestore.collection("users").document(userRecord.getUid());
        docRef.set(userData).get(); // .get() pour attendre la completion
    }
}