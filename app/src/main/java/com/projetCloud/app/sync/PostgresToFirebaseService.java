package com.projetCloud.app.sync;

import com.google.cloud.firestore.*;
import com.projetCloud.app.photos.Photo;
import com.projetCloud.app.photos.PhotoRepository;
import com.projetCloud.app.signalements.Signalement;
import com.projetCloud.app.signalements.SignalementRepository;
import com.projetCloud.app.sync.dto.PostgresReportDTO;
import com.projetCloud.app.utilisateurs.Utilisateur;
import com.projetCloud.app.entreprises.EntrepriseRepository;
import com.projetCloud.app.entreprises.Entreprise;
import com.projetCloud.app.notifications.FcmNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Service pour synchroniser les données PostgreSQL vers Firebase
 * UPSERT: UPDATE si existe (firebase_id), INSERT sinon
 */
@Service
public class PostgresToFirebaseService {

    @Autowired
    private Firestore firestore;

    @Autowired
    private SignalementRepository signalementRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private FcmNotificationService fcmNotificationService;

    /**
     * Synchronise un report PostgreSQL vers Firebase
     * UPSERT: UPDATE si firebase_id existe, INSERT sinon
     * Envoie une notification si le status a changé
     */
    public void syncReportToFirebase(Signalement postgresSignalement) throws ExecutionException, InterruptedException {
        if (postgresSignalement == null || postgresSignalement.getUtilisateur() == null) {
            throw new IllegalArgumentException("Signalement ou utilisateur invalide");
        }

        // Récupérer l'ancien status depuis Firebase (pour détecter les changements)
        String oldStatus = null;
        if (postgresSignalement.getFirebaseId() != null && !postgresSignalement.getFirebaseId().isEmpty()) {
            try {
                DocumentSnapshot firebaseDoc = firestore
                    .collection("reports")
                    .document(postgresSignalement.getFirebaseId())
                    .get()
                    .get();
                if (firebaseDoc.exists()) {
                    oldStatus = firebaseDoc.getString("status");
                }
            } catch (Exception e) {
                System.err.println("[Sync] Erreur récupération ancien status: " + e.getMessage());
            }
        }

        // 1. Préparer le document Firebase
        Map<String, Object> reportData = buildReportDocument(postgresSignalement);

        // 2. Déterminer l'ID Firebase (créer ou utiliser existant)
        String firebaseReportId = postgresSignalement.getFirebaseId();
        if (firebaseReportId == null || firebaseReportId.isEmpty()) {
            firebaseReportId = firestore.collection("reports").document().getId();
        }

        // 3. Créer/mettre à jour le document report dans Firebase
        firestore
            .collection("reports")
            .document(firebaseReportId)
            .set(reportData, SetOptions.merge())
            .get(); // Attendre la completion

        // 4. Mettre à jour le firebase_id dans PostgreSQL si nouveau
        if (postgresSignalement.getFirebaseId() == null || postgresSignalement.getFirebaseId().isEmpty()) {
            postgresSignalement.setFirebaseId(firebaseReportId);
            postgresSignalement.setSyncedAt(LocalDateTime.now());
            postgresSignalement.setIsSyncedToFirebase(true);
            signalementRepository.save(postgresSignalement);
        }

        // 5. Envoyer une notification si le status a changé
        String newStatus = getStatusLabel(postgresSignalement.getIdStatus());
        if (oldStatus != null && !oldStatus.equalsIgnoreCase(newStatus)) {
            sendStatusChangeNotification(postgresSignalement, oldStatus, newStatus);
        }

        // 5. Synchroniser les photos
        syncPhotosToFirebase(firebaseReportId, postgresSignalement);

        System.out.println("[Sync] PostgreSQL → Firebase: Report " + postgresSignalement.getId() + " synced to " + firebaseReportId);
    }

    /**
     * Synchronise toutes les signalements PostgreSQL vers Firebase
     */
    public int syncAllReportsToFirebase() throws ExecutionException, InterruptedException {
        List<Signalement> signalements = signalementRepository.findAll();
        int count = 0;

        for (Signalement signalement : signalements) {
            try {
                syncReportToFirebase(signalement);
                count++;
            } catch (Exception e) {
                System.err.println("Erreur sync signalement " + signalement.getId() + ": " + e.getMessage());
            }
        }

        return count;
    }

    /**
     * Construit le document Firebase à partir d'une Signalement PostgreSQL
     * Inclut tous les champs y compris optionnels
     */
    private Map<String, Object> buildReportDocument(Signalement signalement) {
        Map<String, Object> doc = new HashMap<>();

        // Champs obligatoires
        doc.put("uid", signalement.getUtilisateur().getFirebaseUid());
        doc.put("description", signalement.getDescription() != null ? signalement.getDescription() : "");
        doc.put("lat", signalement.getLatitude() != null ? signalement.getLatitude().doubleValue() : 0.0);
        doc.put("lng", signalement.getLongitude() != null ? signalement.getLongitude().doubleValue() : 0.0);

        // Champs optionnels - tous inclus même s'ils sont null
        doc.put("surfaceM2", signalement.getSurfaceM2() != null ? signalement.getSurfaceM2().doubleValue() : null);
        doc.put("budgetEstimated", signalement.getBudget() != null ? signalement.getBudget().doubleValue() : null);
        doc.put("niveau", signalement.getNiveau()); // Champ niveau (peut être null)
        System.out.println("[Sync] Niveau envoyé vers Firebase: " + signalement.getNiveau());

        // Status et type (libellés)
        if (signalement.getIdStatus() != null && signalement.getIdStatus() > 0) {
            doc.put("status", getStatusLabel(signalement.getIdStatus()));
        }

        if (signalement.getTypeSignalement() != null && signalement.getTypeSignalement().getLibelle() != null) {
            doc.put("type", signalement.getTypeSignalement().getLibelle());
        }

        // Champ companyName (optionnel de Firebase)
        if (signalement.getIdEntreprise() != null && signalement.getIdEntreprise() > 0) {
            doc.put("companyName", getCompanyName(signalement.getIdEntreprise()));
        } else {
            doc.put("companyName", null);
        }

        // Timestamps
        if (signalement.getCreatedAt() != null) {
            doc.put("createdAt", com.google.cloud.Timestamp.of(
                java.util.Date.from(signalement.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())
            ));
        }

        // Synchronisation metadata
        doc.put("syncedAt", com.google.cloud.Timestamp.now());
        doc.put("lastUpdatedFrom", "PostgreSQL");

        return doc;
    }

    /**
     * Synchronise les photos pour un report Firebase
     */
    private void syncPhotosToFirebase(String firebaseReportId, Signalement postgresSignalement) 
            throws ExecutionException, InterruptedException {
        
        // 1. Récupérer les photos PostgreSQL
        List<Photo> postgresPhotos = photoRepository.findBySignalementId(postgresSignalement.getId());

        if (postgresPhotos == null || postgresPhotos.isEmpty()) {
            // Supprimer les photos Firebase si aucune en PostgreSQL
            firestore
                .collection("reports")
                .document(firebaseReportId)
                .collection("photos")
                .get()
                .get()
                .forEach(doc -> {
                    try {
                        doc.getReference().delete().get();
                    } catch (InterruptedException | ExecutionException e) {
                        System.err.println("Erreur suppression photo Firebase: " + e.getMessage());
                    }
                });
            return;
        }

        // 2. Créer/mettre à jour les photos dans Firebase
        for (Photo postgresPhoto : postgresPhotos) {
            try {
                Map<String, Object> photoDoc = new HashMap<>();
                photoDoc.put("url", postgresPhoto.getUrl() != null ? postgresPhoto.getUrl() : "");
                photoDoc.put("imgbbUrl", postgresPhoto.getUrl()); // Alias pour ImgBB
                photoDoc.put("fileName", postgresPhoto.getFileName());
                photoDoc.put("mimeType", postgresPhoto.getMimeType());

                if (postgresPhoto.getUploadedAt() != null) {
                    photoDoc.put("uploadedAt", com.google.cloud.Timestamp.of(
                        java.util.Date.from(postgresPhoto.getUploadedAt().atZone(ZoneId.systemDefault()).toInstant())
                    ));
                }

                // Utiliser l'ID PostgreSQL comme doc ID
                firestore
                    .collection("reports")
                    .document(firebaseReportId)
                    .collection("photos")
                    .document(String.valueOf(postgresPhoto.getId()))
                    .set(photoDoc, SetOptions.merge())
                    .get();

                System.out.println("[Sync] Photo " + postgresPhoto.getId() + " synced to Firebase");
            } catch (Exception e) {
                System.err.println("Erreur sync photo " + postgresPhoto.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Obtient le libellé du status par ID
     */
    private String getStatusLabel(Long statusId) {
        // Map des IDs vers libellés (doit correspondre à la DB)
        Map<Long, String> statusMap = new HashMap<>();
        statusMap.put(1L, "Actif");
        statusMap.put(2L, "Bloqué");
        statusMap.put(3L, "Inactif");
        statusMap.put(4L, "Nouveau");
        statusMap.put(5L, "En cours");
        statusMap.put(6L, "Terminé");
        statusMap.put(7L, "Annulé");
        statusMap.put(8L, "Créé");

        return statusMap.getOrDefault(statusId, "Inconnu");
    }

    /**
     * Obtient le nom de l'entreprise par ID
     */
    private String getCompanyName(Long entrepriseId) {
        if (entrepriseId == null || entrepriseId <= 0) {
            return null;
        }
        try {
            Optional<Entreprise> entreprise = entrepriseRepository.findById(entrepriseId);
            if (entreprise.isPresent()) {
                return entreprise.get().getNom();
            }
        } catch (Exception e) {
            System.err.println("Erreur récupération entreprise " + entrepriseId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Envoie une notification FCM si le status a changé
     */
    private void sendStatusChangeNotification(Signalement signalement, String oldStatus, String newStatus) {
        try {
            // Récupérer le FCM token de l'utilisateur
            Utilisateur utilisateur = signalement.getUtilisateur();
            if (utilisateur == null || utilisateur.getFirebaseUid() == null) {
                System.err.println("[Notification] Utilisateur ou Firebase UID invalide");
                return;
            }

            // Récupérer le FCM token depuis Firebase
            DocumentSnapshot userDoc = firestore
                .collection("users")
                .document(utilisateur.getFirebaseUid())
                .get()
                .get();

            String fcmToken = userDoc.exists() ? userDoc.getString("fcmToken") : null;

            if (fcmToken == null || fcmToken.isEmpty()) {
                System.out.println("[Notification] Pas de FCM token pour l'utilisateur: " + utilisateur.getFirebaseUid());
                return;
            }

            // Construire le type de signalement (ex: "Trou")
            String reportType = signalement.getTypeSignalement() != null 
                ? signalement.getTypeSignalement().getLibelle() 
                : "Signalement";

            // Envoyer la notification
            fcmNotificationService.notifyStatusChange(fcmToken, reportType, oldStatus, newStatus);

        } catch (Exception e) {
            System.err.println("[Notification] Erreur envoi notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

