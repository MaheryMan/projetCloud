package com.projetCloud.app.sync;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Controller pour la synchronisation des données
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/sync")
public class SyncController {

    @Autowired
    private SyncService syncService;

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
     * Endpoint pour vérifier s'il y a des données en attente de synchronisation
     * @return ResponseEntity avec l'état de la synchronisation
     */
    @GetMapping("/status")
    @Operation(summary = "Vérifier le statut de synchronisation", description = "Vérifie s'il y a des données à synchroniser")
    public ResponseEntity<?> getSyncStatus() {
        boolean hasPending = syncService.hasPendingSync();
        return ResponseEntity.ok(new SyncStatusResponse(hasPending));
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

        public SyncStatusResponse(boolean hasPendingSync) {
            this.hasPendingSync = hasPendingSync;
        }

        public boolean isHasPendingSync() { return hasPendingSync; }
        public void setHasPendingSync(boolean hasPendingSync) { this.hasPendingSync = hasPendingSync; }
    }
}