package com.projetCloud.app.sync;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.projetCloud.app.config.ConnectivityService;
import com.projetCloud.app.status.Status;
import com.projetCloud.app.status.StatusRepository;
import com.projetCloud.app.entreprises.Entreprise;
import com.projetCloud.app.entreprises.EntrepriseRepository;
import com.projetCloud.app.typesSignalement.TypeSignalement;
import com.projetCloud.app.typesSignalement.TypeSignalementRepository;
import com.projetCloud.app.configurations.Configuration;
import com.projetCloud.app.configurations.ConfigurationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.api.core.ApiFuture;

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
    private TypeSignalementRepository typeSignalementRepository;

    @Autowired
    private ConfigurationRepository configurationRepository;

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

            // Synchroniser types_signalement
            System.out.println("DEBUG: Starting types_signalement sync");
            totalSynced += syncTypesSignalementToFirebase();
            System.out.println("DEBUG: Types signalement sync completed, total synced: " + totalSynced);

            // Synchroniser configurations
            System.out.println("DEBUG: Starting configurations sync");
            totalSynced += syncConfigurationsToFirebase();
            System.out.println("DEBUG: Configurations sync completed, total synced: " + totalSynced);

            // Synchroniser depuis Firebase (cas redéploiement)
            System.out.println("DEBUG: Starting reverse sync from Firebase");
            totalSynced += syncStatusFromFirebase();
            totalSynced += syncEntreprisesFromFirebase();
            totalSynced += syncTypesSignalementFromFirebase();
            System.out.println("DEBUG: Reverse sync completed, total synced: " + totalSynced);

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

                // Vérifier si le document existe déjà dans Firebase
                ApiFuture<DocumentSnapshot> futureDoc = docRef.get();
                DocumentSnapshot existingDoc = futureDoc.get();

                if (existingDoc.exists()) {
                    // Document existe déjà, ne pas réécrire, juste marquer comme synced
                    status.setIsSyncedToFirebase(true);
                    status.setLastSyncedAt(LocalDateTime.now());
                    statusRepository.save(status);
                    syncedCount++;
                    continue;
                }

                // Document n'existe pas, écrire les données
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

                // Vérifier si le document existe déjà dans Firebase
                ApiFuture<DocumentSnapshot> futureDoc = docRef.get();
                DocumentSnapshot existingDoc = futureDoc.get();

                if (existingDoc.exists()) {
                    // Document existe déjà, ne pas réécrire, juste marquer comme synced
                    entreprise.setIsSyncedToFirebase(true);
                    entreprise.setLastSyncedAt(LocalDateTime.now());
                    entrepriseRepository.save(entreprise);
                    syncedCount++;
                    continue;
                }

                // Document n'existe pas, écrire les données
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
        List<TypeSignalement> allTypes = typeSignalementRepository.findAll();
        
        boolean statusHasPending = allStatus.stream()
                .anyMatch(s -> s.getIsSyncedToFirebase() == null || !s.getIsSyncedToFirebase());
        boolean entreprisesHasPending = allEntreprises.stream()
                .anyMatch(e -> e.getIsSyncedToFirebase() == null || !e.getIsSyncedToFirebase());
        boolean typesHasPending = allTypes.stream()
                .anyMatch(t -> t.getIsSyncedToFirebase() == null || !t.getIsSyncedToFirebase());
        
        return statusHasPending || entreprisesHasPending || typesHasPending;
    }

    /**
     * Synchronise les types_signalement de PostgreSQL vers Firebase
     * @throws TimeoutException 
     */
    public int syncTypesSignalementToFirebase() throws RuntimeException, TimeoutException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        List<TypeSignalement> allTypes = typeSignalementRepository.findAll();
        int syncedCount = 0;

        for (TypeSignalement type : allTypes) {
            try {
                if (type.getIsSyncedToFirebase() != null && type.getIsSyncedToFirebase()) {
                    continue;
                }

                DocumentReference docRef = firestore.collection("metadata")
                        .document("type_signalement_" + type.getId());

                // Vérifier si le document existe déjà dans Firebase
                ApiFuture<DocumentSnapshot> futureDoc = docRef.get();
                DocumentSnapshot existingDoc = futureDoc.get();

                if (existingDoc.exists()) {
                    // Document existe déjà, ne pas réécrire, juste marquer comme synced
                    type.setIsSyncedToFirebase(true);
                    type.setLastSyncedAt(LocalDateTime.now());
                    typeSignalementRepository.save(type);
                    syncedCount++;
                    continue;
                }

                // Document n'existe pas, écrire les données
                Map<String, Object> typeData = new HashMap<>();
                typeData.put("id", type.getId());
                typeData.put("libelle", type.getLibelle());
                typeData.put("description", type.getDescription());
                typeData.put("icone", type.getIcone());
                typeData.put("couleur", type.getCouleur());
                typeData.put("niveauUrgence", type.getNiveauUrgence());
                typeData.put("createdAt", com.google.cloud.Timestamp.now());
                typeData.put("syncedAt", com.google.cloud.Timestamp.now());

                docRef.set(typeData);

                // Mettre à jour PostgreSQL
                type.setIsSyncedToFirebase(true);
                type.setLastSyncedAt(LocalDateTime.now());
                typeSignalementRepository.save(type);

                syncedCount++;
            } catch (Exception e) {
                System.err.println("Erreur lors de la sync du type signalement " + type.getId() + ": " + e.getMessage());
            }
        }

        return syncedCount;
    }

    /**
     * Synchronise les status de Firebase vers PostgreSQL (cas redéploiement)
     */
    public int syncStatusFromFirebase() throws RuntimeException, TimeoutException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        int syncedCount = 0;
        try {
            ApiFuture<QuerySnapshot> query = firestore.collection("metadata").get();
            QuerySnapshot snapshot = query.get();
            
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String docId = doc.getId();
                if (!docId.startsWith("status_")) {
                    continue;
                }

                try {
                    Long id = Long.parseLong(docId.substring(7)); // Extraire l'ID de "status_1"
                    
                    // Vérifier si existe déjà dans PostgreSQL
                    if (statusRepository.existsById(id)) {
                        continue;
                    }

                    Status status = new Status();
                    status.setId(id);
                    status.setCode((String) doc.get("code"));
                    status.setLibelle((String) doc.get("libelle"));
                    status.setIsSyncedToFirebase(true);
                    status.setLastSyncedAt(LocalDateTime.now());
                    
                    statusRepository.save(status);
                    syncedCount++;
                } catch (Exception e) {
                    System.err.println("Erreur lors de la sync du status depuis Firebase " + docId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la sync des status depuis Firebase: " + e.getMessage());
        }

        return syncedCount;
    }

    /**
     * Synchronise les entreprises de Firebase vers PostgreSQL (cas redéploiement)
     */
    public int syncEntreprisesFromFirebase() throws RuntimeException, TimeoutException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        int syncedCount = 0;
        try {
            ApiFuture<QuerySnapshot> query = firestore.collection("metadata").get();
            QuerySnapshot snapshot = query.get();
            
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String docId = doc.getId();
                if (!docId.startsWith("entreprise_")) {
                    continue;
                }

                try {
                    Long id = Long.parseLong(docId.substring(11)); // Extraire l'ID de "entreprise_1"
                    
                    // Vérifier si existe déjà dans PostgreSQL
                    if (entrepriseRepository.existsById(id)) {
                        continue;
                    }

                    Entreprise entreprise = new Entreprise();
                    entreprise.setId(id);
                    entreprise.setNom((String) doc.get("nom"));
                    entreprise.setContactEmail((String) doc.get("contactEmail"));
                    entreprise.setContactTelephone((String) doc.get("contactTelephone"));
                    entreprise.setAdresse((String) doc.get("adresse"));
                    entreprise.setIsSyncedToFirebase(true);
                    entreprise.setLastSyncedAt(LocalDateTime.now());
                    
                    entrepriseRepository.save(entreprise);
                    syncedCount++;
                } catch (Exception e) {
                    System.err.println("Erreur lors de la sync de l'entreprise depuis Firebase " + docId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la sync des entreprises depuis Firebase: " + e.getMessage());
        }

        return syncedCount;
    }

    /**
     * Synchronise les types_signalement de Firebase vers PostgreSQL (cas redéploiement)
     */
    public int syncTypesSignalementFromFirebase() throws RuntimeException, TimeoutException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        int syncedCount = 0;
        try {
            ApiFuture<QuerySnapshot> query = firestore.collection("metadata").get();
            QuerySnapshot snapshot = query.get();
            
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String docId = doc.getId();
                if (!docId.startsWith("type_signalement_")) {
                    continue;
                }

                try {
                    Long id = Long.parseLong(docId.substring(17)); // Extraire l'ID de "type_signalement_1"
                    
                    // Vérifier si existe déjà dans PostgreSQL
                    if (typeSignalementRepository.existsById(id)) {
                        continue;
                    }

                    TypeSignalement type = new TypeSignalement();
                    type.setId(id);
                    type.setLibelle((String) doc.get("libelle"));
                    type.setDescription((String) doc.get("description"));
                    type.setIcone((String) doc.get("icone"));
                    type.setCouleur((String) doc.get("couleur"));
                    Object niveauObj = doc.get("niveauUrgence");
                    if (niveauObj instanceof Long) {
                        type.setNiveauUrgence(((Long) niveauObj).intValue());
                    } else if (niveauObj instanceof Integer) {
                        type.setNiveauUrgence((Integer) niveauObj);
                    }
                    type.setIsSyncedToFirebase(true);
                    type.setLastSyncedAt(LocalDateTime.now());
                    
                    typeSignalementRepository.save(type);
                    syncedCount++;
                } catch (Exception e) {
                    System.err.println("Erreur lors de la sync du type signalement depuis Firebase " + docId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la sync des types_signalement depuis Firebase: " + e.getMessage());
        }

        return syncedCount;
    }

    /**
     * Synchronise les configurations (sécurité) de PostgreSQL vers Firebase
     * Structure: /config/security avec tentatives_max, reset_after_success, etc.
     */
    public int syncConfigurationsToFirebase() throws RuntimeException, TimeoutException {
        if (!connectivityService.isFirebaseOnline()) {
            throw new RuntimeException("Firebase n'est pas accessible");
        }

        int syncedCount = 0;

        try {
            // Récupérer les configurations de sécurité importantes
            Map<String, Object> securityConfig = new HashMap<>();

            // tentatives_max
            String tentativesMax = configurationRepository.findByCle("tentatives_max")
                    .map(Configuration::getValeur)
                    .orElse("3");
            securityConfig.put("tentatives_max", Integer.parseInt(tentativesMax));

            // reset_after_success
            securityConfig.put("reset_after_success", true);

            // message_blocked
            securityConfig.put("message_blocked", "Compte bloqué. Contactez un manager.");

            // Synchroniser vers Firebase
            DocumentReference securityDocRef = firestore.collection("config")
                    .document("security");

            securityDocRef.set(securityConfig);
            syncedCount++;

            System.out.println("DEBUG: Configurations sécurité synchronisées vers Firebase: " + securityConfig);

        } catch (Exception e) {
            System.err.println("Erreur lors de la sync des configurations: " + e.getMessage());
            throw new RuntimeException("Erreur lors de la sync des configurations: " + e.getMessage());
        }

        return syncedCount;
    }
}

