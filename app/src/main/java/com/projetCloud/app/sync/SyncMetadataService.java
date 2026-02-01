package com.projetCloud.app.sync;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.projetCloud.app.config.ConnectivityService;
import com.projetCloud.app.status.Status;
import com.projetCloud.app.status.StatusRepository;
import com.projetCloud.app.entreprises.Entreprise;
import com.projetCloud.app.entreprises.EntrepriseRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Service pour synchroniser les métadonnées (status, entreprises) entre PostgreSQL et Firebase
 * Utilise l'ID PostgreSQL directement comme ID dans Firebase
 */
@Service
public class SyncMetadataService {

    @Autowired
    private ConnectivityService connectivityService;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private Firestore firestore;

    /**
     * Synchronise tous les métadonnées (status, entreprises)
     * @return nombre total d'éléments synchronisés
     * @throws TimeoutException 
     */
    public int syncAllMetadata() throws RuntimeException, TimeoutException {
        System.out.println("DEBUG: syncAllMetadata started");
        
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }
        
        System.out.println("DEBUG: Firebase is online");

        int totalSynced = 0;

        try {
            // Synchroniser status
            System.out.println("DEBUG: Starting status sync");
            totalSynced += syncStatusToFirebase();
            System.out.println("DEBUG: Status sync completed, synced: " + totalSynced);
            
            // Synchroniser entreprises
            System.out.println("DEBUG: Starting entreprises sync");
            totalSynced += syncEntreprisesToFirebase();
            System.out.println("DEBUG: Entreprises sync completed, total synced: " + totalSynced);

            return totalSynced;
        } catch (Exception e) {
            System.err.println("DEBUG: Exception caught: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la sync des métadonnées: " + e.getMessage());
        }
    }

    /**
     * Synchronise les status de PostgreSQL vers Firebase
     * @throws TimeoutException 
     */
    public int syncStatusToFirebase() throws RuntimeException, TimeoutException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        List<Status> allStatus = statusRepository.findAll();
        int syncedCount = 0;

        for (Status status : allStatus) {
            try {
                if (status.getIsSyncedToFirebase() != null && status.getIsSyncedToFirebase()) {
                    continue;
                }

                DocumentReference docRef = firestore.collection("metadata")
                        .document("status_" + status.getId());

                Map<String, Object> statusData = new HashMap<>();
                statusData.put("id", status.getId());
                statusData.put("code", status.getCode());
                statusData.put("libelle", status.getLibelle());
                statusData.put("createdAt", com.google.cloud.Timestamp.now());
                statusData.put("syncedAt", com.google.cloud.Timestamp.now());

                docRef.set(statusData);

                // Mettre à jour PostgreSQL
                status.setIsSyncedToFirebase(true);
                status.setLastSyncedAt(LocalDateTime.now());
                statusRepository.save(status);

                syncedCount++;
            } catch (Exception e) {
                System.err.println("Erreur lors de la sync du status " + status.getId() + ": " + e.getMessage());
            }
        }

        return syncedCount;
    }

    /**
     * Synchronise les entreprises de PostgreSQL vers Firebase
     * @throws TimeoutException 
     */
    public int syncEntreprisesToFirebase() throws RuntimeException, TimeoutException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        List<Entreprise> allEntreprises = entrepriseRepository.findAll();
        int syncedCount = 0;

        for (Entreprise entreprise : allEntreprises) {
            try {
                if (entreprise.getIsSyncedToFirebase() != null && entreprise.getIsSyncedToFirebase()) {
                    continue;
                }

                DocumentReference docRef = firestore.collection("metadata")
                        .document("entreprise_" + entreprise.getId());

                Map<String, Object> entrepriseData = new HashMap<>();
                entrepriseData.put("id", entreprise.getId());
                entrepriseData.put("nom", entreprise.getNom());
                entrepriseData.put("contactEmail", entreprise.getContactEmail());
                entrepriseData.put("contactTelephone", entreprise.getContactTelephone());
                entrepriseData.put("adresse", entreprise.getAdresse());
                entrepriseData.put("createdAt", com.google.cloud.Timestamp.now());
                entrepriseData.put("syncedAt", com.google.cloud.Timestamp.now());

                docRef.set(entrepriseData);

                // Mettre à jour PostgreSQL
                entreprise.setIsSyncedToFirebase(true);
                entreprise.setLastSyncedAt(LocalDateTime.now());
                entrepriseRepository.save(entreprise);

                syncedCount++;
            } catch (Exception e) {
                System.err.println("Erreur lors de la sync de l'entreprise " + entreprise.getId() + ": " + e.getMessage());
            }
        }

        return syncedCount;
    }

    /**
     * Vérifie s'il y a des métadonnées non synchronisées
     */
    public boolean hasPendingMetadataSync() {
        List<Status> allStatus = statusRepository.findAll();
        List<Entreprise> allEntreprises = entrepriseRepository.findAll();
        
        boolean statusHasPending = allStatus.stream()
                .anyMatch(s -> s.getIsSyncedToFirebase() == null || !s.getIsSyncedToFirebase());
        boolean entreprisesHasPending = allEntreprises.stream()
                .anyMatch(e -> e.getIsSyncedToFirebase() == null || !e.getIsSyncedToFirebase());
        
        return statusHasPending || entreprisesHasPending;
    }
}
