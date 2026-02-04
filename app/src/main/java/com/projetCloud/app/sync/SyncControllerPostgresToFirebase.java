package com.projetCloud.app.sync;

import com.projetCloud.app.signalements.Signalement;
import com.projetCloud.app.signalements.SignalementRepository;
import com.projetCloud.app.sync.dto.SyncResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Contrôleur pour synchroniser les données PostgreSQL vers Firebase
 * Inverse du FirebaseToPostgresSyncService
 */
@RestController
@RequestMapping("/api/sync")
public class SyncControllerPostgresToFirebase {

    @Autowired
    private PostgresToFirebaseService postgresFirebaseService;

    @Autowired
    private SignalementRepository signalementRepository;

    /**
     * Synchronise tous les signalements PostgreSQL vers Firebase
     * POST /api/sync/postgres-to-firebase
     */
    @PostMapping("/postgres-to-firebase")
    public ResponseEntity<SyncResponseDTO> syncAllPostgresToFirebase() {
        try {
            int count = postgresFirebaseService.syncAllReportsToFirebase();
            
            SyncResponseDTO response = new SyncResponseDTO();
            response.setStatus("success");
            response.setTotalProcessed(count);
            response.setSuccessCount(count);
            response.setErrorCount(0);
            response.setMessage("PostgreSQL → Firebase: " + count + " signalement(s) synchronisé(s)");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            SyncResponseDTO response = new SyncResponseDTO();
            response.setStatus("error");
            response.setMessage("Erreur lors de la synchronisation: " + e.getMessage());
            response.addError(e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Synchronise un signalement spécifique PostgreSQL vers Firebase
     * POST /api/sync/postgres-to-firebase/{reportId}
     */
    @PostMapping("/postgres-to-firebase/{reportId}")
    public ResponseEntity<SyncResponseDTO> syncSpecificPostgresToFirebase(@PathVariable Long reportId) {
        try {
            Optional<Signalement> signalementOpt = signalementRepository.findById(reportId);

            if (!signalementOpt.isPresent()) {
                SyncResponseDTO response = new SyncResponseDTO();
                response.setStatus("error");
                response.setMessage("Signalement " + reportId + " non trouvé");
                return ResponseEntity.status(404).body(response);
            }

            postgresFirebaseService.syncReportToFirebase(signalementOpt.get());

            SyncResponseDTO response = new SyncResponseDTO();
            response.setStatus("success");
            response.setTotalProcessed(1);
            response.setSuccessCount(1);
            response.setErrorCount(0);
            response.setMessage("PostgreSQL → Firebase: Signalement " + reportId + " synchronisé");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            SyncResponseDTO response = new SyncResponseDTO();
            response.setStatus("error");
            response.setMessage("Erreur lors de la synchronisation: " + e.getMessage());
            response.addError(e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }
}
