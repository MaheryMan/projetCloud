package com.projetCloud.app.sync;

import com.projetCloud.app.signalements.Signalement;
import com.projetCloud.app.sync.dto.FirebasePhotoDTO;
import com.projetCloud.app.sync.dto.FirebaseReportDTO;
import com.projetCloud.app.sync.dto.SyncResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Contrôleur pour les opérations de synchronisation Firebase ↔ PostgreSQL
 */
@RestController
@RequestMapping("/api/sync")
public class SyncControllerFirebasePostgres {

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private FirebaseToPostgresSyncService firebaseToPostgresSyncService;

    /**
     * Synchronise tous les reports de Firebase vers PostgreSQL
     * POST /api/sync/firebase-to-postgres
     */
    @PostMapping("/firebase-to-postgres")
    public ResponseEntity<SyncResponseDTO> syncFirebaseToPostgres() {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;

        try {
            // 1. Récupérer tous les reports de Firebase
            List<FirebaseReportDTO> firebaseReports = firebaseService.getAllReportsFromFirebase();
            int totalProcessed = firebaseReports.size();

            // 2. Synchroniser chaque report
            for (FirebaseReportDTO firebaseReport : firebaseReports) {
                try {
                    // Créer le signalement dans PostgreSQL
                    Signalement createdSignalement = firebaseToPostgresSyncService.syncReportFromFirebase(firebaseReport);

                    // Récupérer et créer les photos
                    List<FirebasePhotoDTO> firebasePhotos = firebaseService.getPhotosForReport(firebaseReport.getId());
                    firebaseToPostgresSyncService.syncPhotosForReport(
                            firebaseReport.getId(),
                            createdSignalement,
                            firebasePhotos
                    );

                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = String.format(
                            "Erreur sync report %s: %s",
                            firebaseReport.getId(),
                            e.getMessage()
                    );
                    errors.add(errorMsg);
                    System.err.println(errorMsg);
                }
            }

            // 3. Construire la réponse
            String status = (errorCount == 0) ? "success" : (errorCount < totalProcessed) ? "partial" : "error";
            String message = String.format(
                    "Sync terminée: %d/%d reports créés",
                    successCount,
                    totalProcessed
            );

            SyncResponseDTO response = new SyncResponseDTO(
                    status,
                    totalProcessed,
                    successCount,
                    errorCount,
                    errors,
                    message
            );

            return ResponseEntity.ok(response);

        } catch (ExecutionException | InterruptedException e) {
            String errorMsg = "Erreur lecture Firebase: " + e.getMessage();
            List<String> errorList = new ArrayList<>();
            errorList.add(errorMsg);

            SyncResponseDTO response = new SyncResponseDTO(
                    "error",
                    0,
                    0,
                    1,
                    errorList,
                    errorMsg
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Synchronise un report spécifique de Firebase vers PostgreSQL
     * POST /api/sync/firebase-to-postgres/{reportId}
     */
    @PostMapping("/firebase-to-postgres/{reportId}")
    public ResponseEntity<SyncResponseDTO> syncFirebaseReportToPostgres(@PathVariable String reportId) {
        List<String> errors = new ArrayList<>();

        try {
            // Récupérer le report de Firebase
            List<FirebaseReportDTO> allReports = firebaseService.getAllReportsFromFirebase();
            FirebaseReportDTO firebaseReport = allReports.stream()
                    .filter(r -> r.getId().equals(reportId))
                    .findFirst()
                    .orElse(null);

            if (firebaseReport == null) {
                errors.add("Report Firebase non trouvé: " + reportId);
                SyncResponseDTO response = new SyncResponseDTO(
                        "error",
                        1,
                        0,
                        1,
                        errors,
                        "Report non trouvé"
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Synchroniser le report
            Signalement createdSignalement = firebaseToPostgresSyncService.syncReportFromFirebase(firebaseReport);

            // Synchroniser les photos
            List<FirebasePhotoDTO> firebasePhotos = firebaseService.getPhotosForReport(reportId);
            firebaseToPostgresSyncService.syncPhotosForReport(reportId, createdSignalement, firebasePhotos);

            SyncResponseDTO response = new SyncResponseDTO(
                    "success",
                    1,
                    1,
                    0,
                    new ArrayList<>(),
                    "Report synchronisé avec succès"
            );

            return ResponseEntity.ok(response);

        } catch (ExecutionException | InterruptedException e) {
            String errorMsg = "Erreur lecture Firebase: " + e.getMessage();
            errors.add(errorMsg);

            SyncResponseDTO response = new SyncResponseDTO(
                    "error",
                    1,
                    0,
                    1,
                    errors,
                    errorMsg
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            String errorMsg = "Erreur synchronisation: " + e.getMessage();
            errors.add(errorMsg);

            SyncResponseDTO response = new SyncResponseDTO(
                    "error",
                    1,
                    0,
                    1,
                    errors,
                    errorMsg
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
