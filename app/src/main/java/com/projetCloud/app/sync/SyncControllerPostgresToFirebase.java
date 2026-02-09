package com.projetCloud.app.sync;

import com.projetCloud.app.signalements.Signalement;
import com.projetCloud.app.signalements.SignalementRepository;
import com.projetCloud.app.sync.dto.SyncResponseDTO;
import com.projetCloud.app.utilisateurs.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
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

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    /**
     * Synchronise tous les signalements PostgreSQL vers Firebase
     * POST /api/sync/postgres-to-firebase
     */
    @PostMapping("/postgres-to-firebase")
    public ResponseEntity<SyncResponseDTO> syncAllPostgresToFirebase() {
        try {
            // Vérifier s'il y a des utilisateurs (autres que les managers) dans la base avant de synchroniser
            long totalUserCount = utilisateurRepository.count();
            long nonManagerUserCount = utilisateurRepository.countNonManagerUsers();
            
            if (nonManagerUserCount == 0) {
                String errorMessage = "ERREUR: Aucun utilisateur (autre que les managers) trouvé dans la base de données. Veuillez d'abord synchroniser les utilisateurs avant de synchroniser les signalements.";
                System.err.println("[SYNC][PG→FB] " + errorMessage);
                System.err.println("[SYNC][PG→FB] Total utilisateurs: " + totalUserCount + ", Utilisateurs non-managers: " + nonManagerUserCount);
                
                SyncResponseDTO response = new SyncResponseDTO();
                response.setStatus("error");
                response.setMessage(errorMessage);
                List<String> errors = new ArrayList<>();
                errors.add(errorMessage);
                response.setErrors(errors);
                
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            System.out.println("[SYNC][PG→FB] " + nonManagerUserCount + " utilisateur(s) non-manager(s) trouvé(s) dans la base");
            
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
            List<String> errors = new ArrayList<>();
            errors.add(e.getMessage());
            response.setErrors(errors);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
