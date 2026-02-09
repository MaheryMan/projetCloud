package com.projetCloud.app.sync;

import com.google.cloud.firestore.*;
import com.google.cloud.Timestamp;
import com.projetCloud.app.sync.dto.FirebaseReportDTO;
import com.projetCloud.app.sync.dto.FirebasePhotoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Service pour lire les données depuis Firebase Firestore
 */
@Service
public class FirebaseService {

    private static final String REPORTS_COLLECTION = "reports";
    private static final String PHOTOS_COLLECTION = "photos";

    @Autowired
    private Firestore firestore;

    /**
     * Récupère tous les reports de Firebase
     */
    public List<FirebaseReportDTO> getAllReportsFromFirebase() throws ExecutionException, InterruptedException {
        try {
            QuerySnapshot reportSnapshots = firestore
                    .collection(REPORTS_COLLECTION)
                    .get()
                    .get();

            List<FirebaseReportDTO> reports = new ArrayList<>();

            for (DocumentSnapshot doc : reportSnapshots.getDocuments()) {
                FirebaseReportDTO report = convertDocumentToReport(doc);
                if (report != null) {
                    reports.add(report);
                }
            }

            return reports;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Erreur lecture Firebase reports: " + e.getMessage(), e);
        }
    }

    /**
     * Récupère les photos d'un report spécifique depuis Firebase
     */
    public List<FirebasePhotoDTO> getPhotosForReport(String reportId) throws ExecutionException, InterruptedException {
        try {
            System.out.println("[FB SERVICE] 🔍 Recherche photos pour reportId: " + reportId);
            
            QuerySnapshot photoSnapshots = firestore
                    .collection(PHOTOS_COLLECTION)
                    .whereEqualTo("reportId", reportId)
                    .get()
                    .get();

            List<FirebasePhotoDTO> photos = new ArrayList<>();

            System.out.println("[FB SERVICE] 📊 Photos trouvées (brutes): " + photoSnapshots.getDocuments().size());
            
            for (DocumentSnapshot doc : photoSnapshots.getDocuments()) {
                FirebasePhotoDTO photo = convertDocumentToPhoto(doc);
                if (photo != null) {
                    photos.add(photo);
                    System.out.println("[FB SERVICE] ✅ Photo ajoutée: ID=" + photo.getId() + 
                        " reportId=" + photo.getReportId() + 
                        " url=" + photo.getImgbbUrl().substring(0, Math.min(30, photo.getImgbbUrl().length())) + "...");
                }
            }
            
            System.out.println("[FB SERVICE] 📊 Photos converties (finales): " + photos.size());
            return photos;
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("[FB SERVICE] ❌ Erreur lecture photos: " + e.getMessage());
            throw new RuntimeException("Erreur lecture Firebase photos: " + e.getMessage(), e);
        }
    }

    /**
     * Convertit un document Firestore en FirebaseReportDTO
     */
    private FirebaseReportDTO convertDocumentToReport(DocumentSnapshot doc) {
        try {
            FirebaseReportDTO report = new FirebaseReportDTO();
            report.setId(doc.getId());
            report.setUid(doc.getString("uid"));
            report.setDescription(doc.getString("description"));
            report.setType(doc.getString("type"));
            report.setStatus(doc.getString("status"));
            report.setCompanyName(doc.getString("companyName"));

            // Convertir les doubles en BigDecimal
            Double lat = doc.getDouble("lat");
            Double lng = doc.getDouble("lng");
            Double surface = doc.getDouble("surfaceM2");
            Double budget = doc.getDouble("budgetEstimated");

            if (lat != null) report.setLat(BigDecimal.valueOf(lat));
            if (lng != null) report.setLng(BigDecimal.valueOf(lng));
            if (surface != null) report.setSurfaceM2(BigDecimal.valueOf(surface));
            if (budget != null) report.setBudgetEstimated(BigDecimal.valueOf(budget));

            // Convertir Timestamp Firestore en LocalDateTime
            Timestamp timestamp = doc.getTimestamp("createdAt");
            if (timestamp != null) {
                report.setCreatedAt(
                        LocalDateTime.ofInstant(
                                timestamp.toDate().toInstant(),
                                ZoneId.systemDefault()
                        )
                );
            }

            return report;
        } catch (Exception e) {
            System.err.println("Erreur conversion report Firebase: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convertit un document Firestore en FirebasePhotoDTO
     */
    private FirebasePhotoDTO convertDocumentToPhoto(DocumentSnapshot doc) {
        try {
            System.out.println("[FB SERVICE] 📄 Conversion document photo brut: " + doc.getId());
            System.out.println("[FB SERVICE]   Données: reportId=" + doc.getString("reportId") + 
                " uid=" + doc.getString("uid") + 
                " imgbbUrl=" + (doc.getString("imgbbUrl") != null ? doc.getString("imgbbUrl").substring(0, Math.min(30, doc.getString("imgbbUrl").length())) + "..." : "NULL"));
            
            FirebasePhotoDTO photo = new FirebasePhotoDTO();
            photo.setId(doc.getId());
            photo.setReportId(doc.getString("reportId"));
            photo.setUid(doc.getString("uid"));
            photo.setImgbbUrl(doc.getString("imgbbUrl"));

            // Convertir Timestamp Firestore en LocalDateTime
            Timestamp timestamp = doc.getTimestamp("uploadedAt");
            if (timestamp != null) {
                photo.setUploadedAt(
                        LocalDateTime.ofInstant(
                                timestamp.toDate().toInstant(),
                                ZoneId.systemDefault()
                        )
                );
            }

            return photo;
        } catch (Exception e) {
            System.err.println("[FB SERVICE] ❌ Erreur conversion photo Firebase: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
