package com.projetCloud.app.utilisateurs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "API pour l'authentification des utilisateurs")
public class UtilisateurController {

    @Autowired
    private UtilisateurService utilisateurService;

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
                                     schema = @Schema(implementation = Utilisateur.class))),
        @ApiResponse(responseCode = "400", description = "Email ou mot de passe incorrect",
                    content = @Content(mediaType = "text/plain"))
    })
    public ResponseEntity<?> login(
            @Parameter(description = "Données d'authentification", required = true)
            @RequestBody LoginRequest loginRequest) {
        Optional<Utilisateur> utilisateur = utilisateurService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());
        return utilisateur.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body("Email ou mot de passe incorrect"));
    }

    /**
     * Classe interne pour la requête de connexion
     */
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
}