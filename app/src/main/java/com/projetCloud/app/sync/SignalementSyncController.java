package com.projetCloud.app.sync;

import com.projetCloud.app.signalements.Signalement;
import com.projetCloud.app.signalements.SignalementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Controller pour gérer la synchronisation bidirectionnelle Firebase <-> Postgres
 * pour les signalements
 */
@RestController
@RequestMapping("/api/sync/signalements")
@CrossOrigin(origins = "*")
public class SignalementSyncController {

    private static final Logger logger = LoggerFactory.getLogger(SignalementSyncController.class);

    @Autowired
    private FirebaseSignalementService firebaseSignalementService;

    @Autowired
    private SignalementRepository signalementRepository;

    /**
     * GET /api/sync/signalements/firebase-pending
     * Récupère les signalements Firebase qui attendent synchronisation (status "Créé")
     */
    @GetMapping("/firebase-pending")
    public ResponseEntity<?> getFirebasePendingReports() {
        try {
            logger.info("Récupération des signalements Firebase en attente");
            List<Map<String, Object>> unsyncedReports = firebaseSignalementService.getUnsyncedFirebaseReports();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", unsyncedReports.size());
            response.put("reports", unsyncedReports);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des signalements Firebase", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * POST /api/sync/signalements/from-firebase
     * Synchronise UN signalement depuis Firebase vers Postgres
     * Body: { "firebaseId": "abc123", ...autres données Firebase... }
     */
    @PostMapping("/from-firebase")
    public ResponseEntity<?> syncFromFirebase(@RequestBody Map<String, Object> firebaseData) {
        try {
            logger.info("Synchronisation signalement depuis Firebase: {}", firebaseData.get("firebaseId"));
            
            if (!firebaseData.containsKey("firebaseId")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "firebaseId requis"));
            }

            Signalement signalement = firebaseSignalementService.syncFirebaseReportToPostgres(firebaseData);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Signalement synchronisé avec succès",
                    "signalementId", signalement.getId(),
                    "firebaseId", signalement.getFirebaseId()
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de la synchronisation depuis Firebase", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * POST /api/sync/signalements/batch-from-firebase
     * Synchronise TOUS les signalements Firebase en attente vers Postgres
     */
    @PostMapping("/batch-from-firebase")
    public ResponseEntity<?> batchSyncFromFirebase() {
        try {
            logger.info("Synchronisation batch depuis Firebase");
            
            List<Map<String, Object>> unsyncedReports = firebaseSignalementService.getUnsyncedFirebaseReports();
            int successCount = 0;
            int errorCount = 0;
            
            for (Map<String, Object> firebaseData : unsyncedReports) {
                try {
                    firebaseSignalementService.syncFirebaseReportToPostgres(firebaseData);
                    successCount++;
                } catch (Exception e) {
                    logger.error("Erreur sync signalement {}: {}", firebaseData.get("firebaseId"), e.getMessage());
                    errorCount++;
                }
            }
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Synchronisation batch terminée",
                    "totalProcessed", unsyncedReports.size(),
                    "successCount", successCount,
                    "errorCount", errorCount
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de la synchronisation batch", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * POST /api/sync/signalements/{id}/to-firebase
     * Synchronise UN signalement depuis Postgres vers Firebase
     * Utilisé quand le manager valide/modifie un signalement
     */
    @PostMapping("/{id}/to-firebase")
    public ResponseEntity<?> syncToFirebase(@PathVariable Long id) {
        try {
            logger.info("Synchronisation signalement {} vers Firebase", id);
            
            Signalement signalement = signalementRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Signalement introuvable: " + id));

            firebaseSignalementService.syncPostgresReportToFirebase(signalement);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Signalement synchronisé vers Firebase",
                    "signalementId", signalement.getId(),
                    "firebaseId", signalement.getFirebaseId()
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de la synchronisation vers Firebase", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * POST /api/sync/signalements/batch-to-firebase
     * Synchronise TOUS les signalements en attente depuis Postgres vers Firebase
     */
    @PostMapping("/batch-to-firebase")
    public ResponseEntity<?> batchSyncToFirebase() {
        try {
            logger.info("Synchronisation batch vers Firebase");
            int syncedCount = firebaseSignalementService.syncAllPendingToFirebase();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Synchronisation batch vers Firebase terminée",
                    "syncedCount", syncedCount
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de la synchronisation batch vers Firebase", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * GET /api/sync/signalements/status
     * Récupère le statut général de synchronisation
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSyncStatus() {
        try {
            List<Map<String, Object>> firebasePending = firebaseSignalementService.getUnsyncedFirebaseReports();
            List<Signalement> postgresPending = signalementRepository.findByNeedsFirebaseSyncTrue();
            List<Signalement> creeStatus = signalementRepository.findByIdStatus(8L); // Status "Créé"
            
            Map<String, Object> status = new HashMap<>();
            status.put("firebasePendingCount", firebasePending.size());
            status.put("postgresPendingCount", postgresPending.size());
            status.put("signalementsCreeCount", creeStatus.size());
            status.put("lastCheck", LocalDateTime.now());
            
            return ResponseEntity.ok(Map.of("success", true, "status", status));
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du statut", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }
}
