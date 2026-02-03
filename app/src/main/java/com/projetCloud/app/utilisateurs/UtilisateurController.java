package com.projetCloud.app.utilisateurs;

import com.projetCloud.app.configurations.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * Controller pour la gestion des utilisateurs
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "API pour l'authentification des utilisateurs")
public class UtilisateurController {

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Endpoint pour la connexion des utilisateurs
     * @param loginRequest objet contenant l'email et le mot de passe de l'utilisateur
     * @return ResponseEntity contenant l'utilisateur connecté ou une erreur si les informations sont incorrectes
     */
    @PostMapping("/login")
    @Operation(summary = "Authentifier un utilisateur", description = "Permet à un utilisateur de se connecter avec son email et mot de passe")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentification réussie",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = AuthService.AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Email ou mot de passe incorrect",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> login(
            @Parameter(description = "Données d'authentification", required = true)
            @RequestBody LoginRequest loginRequest) {
        // Récupérer deviceInfo et ipAddress depuis la requête (simplifié pour l'exemple)
        String deviceInfo = "Web Browser"; // À adapter selon les besoins
        String ipAddress = "127.0.0.1"; // À récupérer depuis HttpServletRequest

        Optional<AuthService.AuthResponse> authResponse = authService.login(
            loginRequest.getEmail(), 
            loginRequest.getPassword(), 
            deviceInfo, 
            ipAddress
        );

        if (authResponse.isPresent()) {
            return ResponseEntity.ok(authResponse.get());
        } else {
            // Gestion des tentatives échouées
            Optional<Utilisateur> user = utilisateurService.findByEmail(loginRequest.getEmail());
            if (user.isPresent()) {
                Utilisateur u = user.get();
                
                // Récupérer le nombre max de tentatives depuis la configuration
                int maxAttempts = Integer.parseInt(configurationService.getValue("tentatives_max", "3"));
                int blocageDurationMinutes = Integer.parseInt(configurationService.getValue("duree_blocage_minutes", "30"));
                
                // Vérifier si l'utilisateur est déjà bloqué et si le délai de blocage est écoulé
                if (u.getIsBlocked() != null && u.getIsBlocked()) {
                    if (u.getLastFailedAttempt() != null) {
                        LocalDateTime blockedUntil = u.getLastFailedAttempt().plusMinutes(blocageDurationMinutes);
                        if (LocalDateTime.now().isAfter(blockedUntil)) {
                            // Débloquer l'utilisateur après expiration du délai
                            u.setIsBlocked(false);
                            u.setTentativesConnexion(0);
                        } else {
                            // L'utilisateur est toujours bloqué
                            long minutesLeft = java.time.Duration.between(LocalDateTime.now(), blockedUntil).toMinutes();
                            return ResponseEntity.status(403).body("Compte bloqué. Réessayez dans " + minutesLeft + " minutes.");
                        }
                    }
                }
                
                // Incrémenter les tentatives échouées
                int attempts = u.getTentativesConnexion() != null ? u.getTentativesConnexion() : 0;
                u.setTentativesConnexion(attempts + 1);
                u.setLastFailedAttempt(LocalDateTime.now());
                
                // Bloquer si nombre max de tentatives atteint
                if (u.getTentativesConnexion() >= maxAttempts) {
                    u.setIsBlocked(true);
                    utilisateurService.save(u);
                    return ResponseEntity.status(403).body("Compte bloqué suite à " + maxAttempts + " tentatives échouées. Réessayez dans " + blocageDurationMinutes + " minutes.");
                }
                
                utilisateurService.save(u);
                int attemptsLeft = maxAttempts - u.getTentativesConnexion();
                return ResponseEntity.badRequest().body("Email ou mot de passe incorrect. Il vous reste " + attemptsLeft + " tentative(s).");
            }
            return ResponseEntity.badRequest().body("Email ou mot de passe incorrect");
        }
    }

    @Schema(description = "Requête d'authentification contenant les informations de connexion", example = """
            {
              "email": "user@example.com",
              "password": "password123"
            }
            """)
    public static class LoginRequest {
        @Schema(description = "Adresse email de l'utilisateur", example = "user@example.com", required = true, format = "email")
        private String email;

        @Schema(description = "Mot de passe de l'utilisateur", example = "password123", required = true, format = "password")
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnecter un utilisateur", description = "Invalide la session de l'utilisateur")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Déconnexion réussie"),
        @ApiResponse(responseCode = "400", description = "Token invalide")
    })
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Token manquant ou invalide");
        }
        String token = authHeader.substring(7); // Enlever "Bearer "
        boolean success = authService.logout(token);
        if (success) {
            return ResponseEntity.ok().body("Déconnexion réussie");
        } else {
            return ResponseEntity.badRequest().body("Token invalide");
        }
    }

    /**
     * Endpoint pour l'inscription d'un nouvel utilisateur
     * @param registerRequest objet contenant les données d'inscription
     * @return ResponseEntity contenant l'utilisateur créé ou une erreur
     */
    @PostMapping("/register")
    @Operation(summary = "Inscrire un nouvel utilisateur", description = "Permet à un utilisateur de s'inscrire avec email, mot de passe, nom et prénom")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Inscription réussie",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = Utilisateur.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou utilisateur existe déjà",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> register(
            @Parameter(description = "Données d'inscription", required = true)
            @RequestBody RegisterRequest registerRequest) {
        try {
            Utilisateur utilisateur = utilisateurService.register(
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getNom(),
                registerRequest.getPrenom(),
                registerRequest.getNumTel()
            );
            return ResponseEntity.status(201).body(utilisateur);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Endpoint pour l'inscription via Google OAuth
     * @param registerGoogleRequest objet contenant le token Google et données optionnelles
     * @return ResponseEntity contenant l'utilisateur créé ou une erreur
     */
    @PostMapping("/register-google")
    @Operation(summary = "Inscrire un nouvel utilisateur via Google", description = "Permet à un utilisateur de s'inscrire avec un token Google ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Inscription réussie",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = Utilisateur.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou utilisateur existe déjà",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> registerWithGoogle(
            @Parameter(description = "Données d'inscription Google", required = true)
            @RequestBody RegisterGoogleRequest registerGoogleRequest) {
        try {
            Utilisateur utilisateur = utilisateurService.registerWithGoogle(
                registerGoogleRequest.getIdToken(),
                registerGoogleRequest.getNom(),
                registerGoogleRequest.getPrenom(),
                registerGoogleRequest.getNumTel()
            );
            return ResponseEntity.status(201).body(utilisateur);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    public static class RegisterGoogleRequest {
        private String idToken;
        private String nom;
        private String prenom;
        private String numTel;

        // Getters et setters
        public String getIdToken() { return idToken; }
        public void setIdToken(String idToken) { this.idToken = idToken; }
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }
        public String getNumTel() { return numTel; }
        public void setNumTel(String numTel) { this.numTel = numTel; }
    }

    public static class RegisterRequest {
        private String email;
        private String password;
        private String nom;
        private String prenom;
        private String numTel;

        // Getters et setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }
        public String getNumTel() { return numTel; }
        public void setNumTel(String numTel) { this.numTel = numTel; }
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse() {
        }

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}