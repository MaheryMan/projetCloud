package com.projetCloud.app.sync;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.projetCloud.app.utilisateurs.Utilisateur;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Controller pour la synchronisation des données
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/sync")
public class SyncController {

    @Autowired
    private SyncService syncService;

    @Autowired
    private SyncMetadataService syncMetadataService;

    /**
     * Endpoint pour synchroniser les utilisateurs locaux vers Firebase
     * @return ResponseEntity avec le résultat de la synchronisation
     */
    @PostMapping("/users")
    @Operation(summary = "Synchroniser les utilisateurs", description = "Synchronise les utilisateurs locaux non synchronisés vers Firebase")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Synchronisation réussie",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = SyncResponse.class))),
        @ApiResponse(responseCode = "400", description = "Erreur lors de la synchronisation")
    })
    public ResponseEntity<?> syncUsers() {
        try {
            int syncedCount = syncService.syncUsersToFirebase();
            return ResponseEntity.ok(new SyncResponse("Synchronisation réussie", syncedCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new SyncResponse("Erreur: " + e.getMessage(), 0));
        }
    }

    /**
     * Endpoint pour synchroniser les utilisateurs depuis Firebase vers PostgreSQL
     * Gère la fusion automatique des utilisateurs existants par email
     * @return ResponseEntity avec le résultat de la synchronisation
     */
    @PostMapping("/users/from-firebase")
    @Operation(summary = "Synchroniser depuis Firebase", description = "Synchronise les utilisateurs Firebase vers PostgreSQL, y compris la fusion des utilisateurs existants par email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Synchronisation réussie",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = SyncResponse.class))),
        @ApiResponse(responseCode = "400", description = "Erreur lors de la synchronisation")
    })
    public ResponseEntity<?> syncUsersFromFirebase() {
        try {
            int syncedCount = syncService.syncUsersFromFirebase();
            return ResponseEntity.ok(new SyncResponse("Synchronisation depuis Firebase réussie", syncedCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new SyncResponse("Erreur: " + e.getMessage(), 0));
        }
    }

    /**
     * Endpoint pour synchroniser les modifications hors ligne
     * @return ResponseEntity avec le résultat de la synchronisation
     */
    @PostMapping("/users/offline-changes")
    @Operation(summary = "Synchroniser les modifications hors ligne", description = "Synchronise les modifications effectuées hors ligne vers Firebase")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Synchronisation réussie",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = SyncResponse.class))),
        @ApiResponse(responseCode = "400", description = "Erreur lors de la synchronisation")
    })
    public ResponseEntity<?> syncOfflineModifications() {
        try {
            int syncedCount = syncService.syncOfflineModifications();
            return ResponseEntity.ok(new SyncResponse("Synchronisation des modifications réussie", syncedCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new SyncResponse("Erreur: " + e.getMessage(), 0));
        }
    }

    /**
     * Endpoint pour synchroniser les métadonnées (status, entreprises, types_signalement)
     * @return ResponseEntity avec le résultat de la synchronisation
     */
    @PostMapping("/metadata")
    @Operation(summary = "Synchroniser les métadonnées", description = "Synchronise les métadonnées (status, entreprises, types_signalement) entre PostgreSQL et Firebase")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Synchronisation réussie",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = SyncResponse.class))),
        @ApiResponse(responseCode = "400", description = "Erreur lors de la synchronisation")
    })
    public ResponseEntity<?> syncMetadata() {
        try {
            int syncedCount = syncMetadataService.syncAllMetadata();
            return ResponseEntity.ok(new SyncResponse("Synchronisation des métadonnées réussie", syncedCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new SyncResponse("Erreur: " + e.getMessage(), 0));
        }
    }

    /**
     * Endpoint pour vérifier s'il y a des données en attente de synchronisation
     * @return ResponseEntity avec l'état de la synchronisation
     */
    @GetMapping("/status")
    @Operation(summary = "Vérifier le statut de synchronisation", description = "Vérifie s'il y a des données à synchroniser")
    public ResponseEntity<?> getSyncStatus() {
        boolean hasPending = syncService.hasPendingSync();
        boolean hasModifiedOffline = syncService.hasOfflineModifications();
        return ResponseEntity.ok(new SyncStatusResponse(hasPending, hasModifiedOffline));
    }

    /**
     * Endpoint pour l'authentification hors ligne (comptes locaux)
     * @param request Requête avec email et password
     * @return ResponseEntity avec token temporaire
     */
    @PostMapping("/auth/offline")
    @Operation(summary = "Authentification hors ligne", description = "Authentifie un utilisateur local hors ligne")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentification réussie",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Authentification échouée")
    })
    public ResponseEntity<?> authenticateOffline(@RequestBody CreateUserRequest request) {
        try {
            Utilisateur user = syncService.authenticateOffline(request.getEmail(), request.getPassword());
            if (user != null) {
                String token = syncService.generateOfflineToken(user);
                return ResponseEntity.ok(Map.of("token", token, "user", user));
            } else {
                return ResponseEntity.status(401).body("Authentification échouée");
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Erreur d'authentification: " + e.getMessage());
        }
    }


        /**
     * Endpoint pour synchroniser bi-directionnellement les signalements
     * Insère les signalements manquants dans PostgreSQL et Firebase
     * @return ResponseEntity avec le résultat de la synchronisation
     */
    @PostMapping("/signalements/bidirectional")
    @Operation(summary = "Synchronisation bi-directionnelle des signalements", description = "Synchronise les signalements manquants entre PostgreSQL et Firebase dans les deux sens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Synchronisation réussie",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = SyncResponse.class))),
        @ApiResponse(responseCode = "400", description = "Erreur lors de la synchronisation")
    })
    public ResponseEntity<?> syncSignalementsBidirectionnel() {
        try {
            int syncedCount = syncService.syncSignalementsBidirectionnel();
            return ResponseEntity.ok(new SyncResponse("Synchronisation bi-directionnelle des signalements réussie", syncedCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new SyncResponse("Erreur: " + e.getMessage(), 0));
        }
    }

    /**
     * Endpoint pour créer un utilisateur en ligne
     * @param request Requête avec email, password, nom, prenom
     * @return ResponseEntity avec l'utilisateur créé
     */
    @PostMapping("/users/online")
    @Operation(summary = "Créer un utilisateur en ligne", description = "Crée un utilisateur directement dans PostgreSQL et Firebase")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur créé",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Erreur lors de la création")
    })
    public ResponseEntity<?> createUserOnline(@RequestBody CreateUserRequest request) {
        try {
            Utilisateur user = syncService.createUserOnline(request.getEmail(), request.getPassword(), request.getNom(), request.getPrenom());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new SyncResponse("Erreur: " + e.getMessage(), 0));
        }
    }

    public static class SyncResponse {
        private String message;
        private int syncedCount;

        public SyncResponse(String message, int syncedCount) {
            this.message = message;
            this.syncedCount = syncedCount;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getSyncedCount() { return syncedCount; }
        public void setSyncedCount(int syncedCount) { this.syncedCount = syncedCount; }
    }

    public static class SyncStatusResponse {
        private boolean hasPendingSync;
        private boolean hasModifiedOffline;

        public SyncStatusResponse(boolean hasPendingSync, boolean hasModifiedOffline) {
            this.hasPendingSync = hasPendingSync;
            this.hasModifiedOffline = hasModifiedOffline;
        }

        public boolean isHasPendingSync() { return hasPendingSync; }
        public void setHasPendingSync(boolean hasPendingSync) { this.hasPendingSync = hasPendingSync; }
        public boolean isHasModifiedOffline() { return hasModifiedOffline; }
        public void setHasModifiedOffline(boolean hasModifiedOffline) { this.hasModifiedOffline = hasModifiedOffline; }
    }

    public static class CreateUserRequest {
        private String email;
        private String password;
        private String nom;
        private String prenom;

        public CreateUserRequest() {}

        public CreateUserRequest(String email, String password, String nom, String prenom) {
            this.email = email;
            this.password = password;
            this.nom = nom;
            this.prenom = prenom;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }
    }
}