package com.projetCloud.app.sync;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.projetCloud.app.signalements.Signalement;
import com.projetCloud.app.signalements.SignalementRepository;
import com.projetCloud.app.typesSignalement.TypeSignalement;
import com.projetCloud.app.typesSignalement.TypeSignalementRepository;
import com.projetCloud.app.utilisateurs.Utilisateur;
import com.projetCloud.app.utilisateurs.UtilisateurRepository;
import com.projetCloud.app.status.Status;
import com.projetCloud.app.status.StatusRepository;
import com.projetCloud.app.entreprises.Entreprise;
import com.projetCloud.app.entreprises.EntrepriseRepository;
import com.projetCloud.app.photos.Photo;
import com.projetCloud.app.photos.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Service pour gérer la synchronisation bidirectionnelle Firebase <-> Postgres
 * pour les signalements
 */
@Service
public class FirebaseSignalementService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseSignalementService.class);
    private static final String REPORTS_COLLECTION = "reports";

    @Autowired
    private Firestore firestore;

    @Autowired
    private SignalementRepository signalementRepository;

    @Autowired
    private TypeSignalementRepository typeSignalementRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private PhotoService photoService;

    /**
     * Récupère les signalements Firebase qui n'ont pas encore été synchronisés en Postgres
     * Synchronise TOUS les signalements, peu importe leur status
     * @return Liste des signalements Firebase non synchronisés
     */
    public List<Map<String, Object>> getUnsyncedFirebaseReports() throws ExecutionException, InterruptedException {
        logger.info("Récupération des signalements Firebase non synchronisés");

        CollectionReference reportsRef = firestore.collection(REPORTS_COLLECTION);
        
        // Récupérer tous les signalements
        ApiFuture<QuerySnapshot> future = reportsRef.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        logger.info("Total documents Firebase trouvés: {}", documents.size());

        List<Map<String, Object>> unsyncedReports = new ArrayList<>();
        int alreadySynced = 0;

        for (QueryDocumentSnapshot document : documents) {
            String firebaseId = document.getId();
            
            // Vérifier si déjà synchronisé en Postgres
            Optional<Signalement> existingSignalement = signalementRepository.findByFirebaseId(firebaseId);
            
            if (existingSignalement.isEmpty()) {
                Map<String, Object> data = document.getData();
                data.put("firebaseId", firebaseId);
                
                String status = (String) data.get("status");
                logger.info("Document {}: status='{}', type='{}', description='{}'", 
                    firebaseId, status, data.get("type"), 
                    data.get("description") != null ? data.get("description").toString().substring(0, Math.min(50, data.get("description").toString().length())) : "null");
                
                // Synchroniser TOUS les signalements non-Postgres
                unsyncedReports.add(data);
                logger.info("✓ Document {} ajouté à la liste de sync", firebaseId);
            } else {
                alreadySynced++;
                logger.info("✗ Document {} déjà synchronisé en Postgres (ID: {})", firebaseId, existingSignalement.get().getId());
            }
        }

        logger.info("Résumé: {} total, {} déjà synchro, {} à synchroniser", 
            documents.size(), alreadySynced, unsyncedReports.size());
        return unsyncedReports;
    }

    /**
     * Synchronise un signalement depuis Firebase vers Postgres
     * @param firebaseData Données du signalement depuis Firebase
     * @return Le signalement créé en Postgres
     */
    public Signalement syncFirebaseReportToPostgres(Map<String, Object> firebaseData) {
        logger.info("Synchronisation signalement Firebase vers Postgres: {}", firebaseData.get("firebaseId"));

        Signalement signalement = new Signalement();
        
        // Données obligatoires
        signalement.setFirebaseId((String) firebaseData.get("firebaseId"));
        signalement.setLatitude(new BigDecimal(firebaseData.get("lat").toString()));
        signalement.setLongitude(new BigDecimal(firebaseData.get("lng").toString()));
        signalement.setDescription((String) firebaseData.get("description"));

        // Données optionnelles
        if (firebaseData.get("surfaceM2") != null) {
            signalement.setSurfaceM2(new BigDecimal(firebaseData.get("surfaceM2").toString()));
        }
        
        // Gestion de la photo
        if (firebaseData.get("photo") != null) {
            String photoUrl = (String) firebaseData.get("photo");
            try {
                // Créer ou trouver la photo dans l'entité Photo
                Photo photo = photoService.findOrCreateByUrl(photoUrl, null, "image/jpeg");
                signalement.addPhoto(photo);
                logger.info("Photo associée au signalement: {}", photoUrl);
            } catch (Exception e) {
                logger.error("Erreur lors de l'association de la photo: {}", e.getMessage());
                // Continue sans photo en cas d'erreur
            }
        }
        
        // Gestion des photos multiples (si Firebase contient un array)
        if (firebaseData.get("photos") != null) {
            try {
                @SuppressWarnings("unchecked")
                List<String> photoUrls = (List<String>) firebaseData.get("photos");
                for (String photoUrl : photoUrls) {
                    if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                        Photo photo = photoService.findOrCreateByUrl(photoUrl.trim(), null, "image/jpeg");
                        signalement.addPhoto(photo);
                        logger.info("Photo multiple associée au signalement: {}", photoUrl);
                    }
                }
            } catch (Exception e) {
                logger.error("Erreur lors de l'association des photos multiples: {}", e.getMessage());
            }
        }

        // Type de signalement
        String typeLibelle = (String) firebaseData.get("type");
        TypeSignalement typeSignalement = typeSignalementRepository.findByLibelleIgnoreCase(typeLibelle)
                .orElseThrow(() -> new RuntimeException("Type de signalement introuvable: " + typeLibelle));
        signalement.setTypeSignalement(typeSignalement);

        // Utilisateur
        String uid = (String) firebaseData.get("uid");
        Utilisateur utilisateur = utilisateurRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + uid));
        signalement.setUtilisateur(utilisateur);

        // Status: utiliser le status depuis Firebase, sinon "Créé" par défaut
        String statusLibelle = (String) firebaseData.get("status");
        if (statusLibelle != null && !statusLibelle.isEmpty()) {
            Long statusId = statusRepository.findByLibelleIgnoreCase(statusLibelle)
                    .map(Status::getId)
                    .orElseGet(() -> {
                        logger.warn("Status '{}' introuvable dans Postgres, utilisation de 'Créé' par défaut", statusLibelle);
                        return 8L; // Créé par défaut
                    });
            signalement.setIdStatus(statusId);
        } else {
            signalement.setIdStatus(8L); // Créé par défaut si pas de status
        }

        // Entreprise: mapper companyName de Firebase à idEntreprise en Postgres
        String companyName = (String) firebaseData.get("companyName");
        logger.info("[ENTREPRISE MAPPING] companyName reçu du Firebase: '{}'", companyName);
        
        if (companyName != null && !companyName.isEmpty()) {
            logger.info("[ENTREPRISE MAPPING] 🔍 Recherche entreprise avec nom: '{}'", companyName);
            
            Entreprise entreprise = entrepriseRepository.findByNomIgnoreCase(companyName)
                    .orElseGet(() -> {
                        logger.warn("[ENTREPRISE MAPPING] ⚠️ Entreprise '{}' introuvable dans Postgres, signalement non attribué", companyName);
                        return null;
                    });
            
            if (entreprise != null) {
                signalement.setIdEntreprise(entreprise.getId());
                logger.info("[ENTREPRISE MAPPING] ✅ Entreprise trouvée et associée: '{}' (ID: {})", companyName, entreprise.getId());
            } else {
                logger.info("[ENTREPRISE MAPPING] ❌ Signalement créé sans entreprise (companyName: '{}')", companyName);
            }
        } else {
            logger.info("[ENTREPRISE MAPPING] ℹ️ Pas de companyName fourni par Firebase");
        }

        // Timestamps
        if (firebaseData.get("createdAt") != null) {
            Object createdAtObj = firebaseData.get("createdAt");
            if (createdAtObj instanceof com.google.cloud.Timestamp) {
                com.google.cloud.Timestamp timestamp = (com.google.cloud.Timestamp) createdAtObj;
                signalement.setCreatedAt(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                    ZoneId.systemDefault()
                ));
            }
        }

        signalement.setSyncedFromFirebaseAt(LocalDateTime.now());
        signalement.setIsSyncedToFirebase(true);

        Signalement saved = signalementRepository.save(signalement);
        logger.info("Signalement synchronisé avec succès: ID Postgres={}, ID Firebase={}, idEntreprise={}", 
                    saved.getId(), saved.getFirebaseId(), saved.getIdEntreprise());

        return saved;
    }

    /**
     * Synchronise un signalement modifié depuis Postgres vers Firebase
     * Utilisé quand le manager valide/modifie un signalement
     * @param signalement Le signalement à synchroniser vers Firebase
     */
    public void syncPostgresReportToFirebase(Signalement signalement) throws ExecutionException, InterruptedException {
        logger.info("Synchronisation signalement Postgres vers Firebase: ID={}", signalement.getId());

        if (signalement.getFirebaseId() == null) {
            throw new RuntimeException("Le signalement n'a pas de firebaseId");
        }

        DocumentReference docRef = firestore.collection(REPORTS_COLLECTION).document(signalement.getFirebaseId());

        Map<String, Object> updates = new HashMap<>();
        
        // Données de base
        updates.put("lat", signalement.getLatitude());
        updates.put("lng", signalement.getLongitude());
        updates.put("description", signalement.getDescription());
        updates.put("type", signalement.getTypeSignalement().getLibelle());
        
        // Status
        String statusLibelle = statusRepository.findById(signalement.getIdStatus())
                .map(s -> s.getLibelle())
                .orElse("Nouveau");
        updates.put("status", statusLibelle);

        // Données optionnelles
        if (signalement.getSurfaceM2() != null) {
            updates.put("surfaceM2", signalement.getSurfaceM2().doubleValue());
        }
        if (signalement.getBudget() != null) {
            updates.put("budgetEstimated", signalement.getBudget().doubleValue());
        }
        if (signalement.getIdEntreprise() != null) {
            String companyName = entrepriseRepository.findById(signalement.getIdEntreprise())
                    .map(e -> e.getNom())
                    .orElse(null);
            updates.put("companyName", companyName);
        }
        
        // Gestion de la photo
        if (!signalement.getPhotos().isEmpty()) {
            // Envoyer la première photo pour compatibilité avec l'ancien format
            updates.put("photo", signalement.getPhotoUrl());
            
            // Envoyer toutes les photos dans un array pour les nouvelles versions
            List<String> photoUrls = signalement.getPhotoUrls();
            updates.put("photos", photoUrls);
            
            logger.info("Photos mises à jour dans Firebase: {} photo(s)", photoUrls.size());
        }

        // Mise à jour dans Firebase
        ApiFuture<WriteResult> future = docRef.update(updates);
        future.get(); // Attendre la fin de la mise à jour

        // Marquer comme synchronisé
        signalement.setSyncedAt(LocalDateTime.now());
        signalement.setIsSyncedToFirebase(true);
        signalement.setNeedsFirebaseSync(false);
        signalementRepository.save(signalement);

        logger.info("Signalement synchronisé vers Firebase avec succès: {}", signalement.getFirebaseId());
    }

    /**
     * Synchronise tous les signalements qui ont besoin d'être envoyés à Firebase
     * @return Nombre de signalements synchronisés
     */
    public int syncAllPendingToFirebase() throws ExecutionException, InterruptedException {
        List<Signalement> pendingSignalements = signalementRepository.findByNeedsFirebaseSyncTrue();
        
        int syncedCount = 0;
        for (Signalement signalement : pendingSignalements) {
            try {
                syncPostgresReportToFirebase(signalement);
                syncedCount++;
            } catch (Exception e) {
                logger.error("Erreur lors de la sync vers Firebase pour signalement ID={}: {}", 
                            signalement.getId(), e.getMessage());
            }
        }

        logger.info("Synchronisé {} signalements vers Firebase", syncedCount);
        return syncedCount;
    }
}
