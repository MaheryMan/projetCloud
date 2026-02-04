package com.projetCloud.app.sync;

import com.projetCloud.app.photos.Photo;
import com.projetCloud.app.photos.PhotoRepository;
import com.projetCloud.app.signalements.Signalement;
import com.projetCloud.app.signalements.SignalementService;
import com.projetCloud.app.status.Status;
import com.projetCloud.app.status.StatusRepository;
import com.projetCloud.app.sync.dto.FirebasePhotoDTO;
import com.projetCloud.app.sync.dto.FirebaseReportDTO;
import com.projetCloud.app.typesSignalement.TypeSignalement;
import com.projetCloud.app.signalements.SignalementRepository;
import com.projetCloud.app.typesSignalement.TypeSignalementRepository;
import com.projetCloud.app.utilisateurs.Utilisateur;
import com.projetCloud.app.utilisateurs.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service pour synchroniser les données Firebase vers PostgreSQL
 */
@Service
public class FirebaseToPostgresSyncService {

    @Autowired
    private SignalementService signalementService;

    @Autowired
    private SignalementRepository signalementRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private TypeSignalementRepository typeSignalementRepository;

    @Autowired
    private PhotoRepository photoRepository;

    /**
     * Synchronise un report Firebase vers PostgreSQL
     * UPSERT: UPDATE si existe (firebase_id), INSERT sinon
     * Crée aussi les photos liées
     */
    public Signalement syncReportFromFirebase(FirebaseReportDTO firebaseReport) throws Exception {
        // 1. Trouver ou créer l'utilisateur
        Utilisateur utilisateur = findOrCreateUserByFirebaseUid(firebaseReport.getUid());

        // 2. Mapper le status (normaliser la comparaison)
        Status status = mapFirebaseStatus(firebaseReport.getStatus());

        // 3. Mapper le type (normaliser la comparaison)
        TypeSignalement typeSignalement = mapFirebaseType(firebaseReport.getType());

        // 4. Vérifier si le signalement existe déjà par firebase_id
        Optional<Signalement> existingSignalement = signalementRepository.findByFirebaseId(firebaseReport.getId());

        Signalement signalement;
        if (existingSignalement.isPresent()) {
            // UPSERT: Mettre à jour le signalement existant
            signalement = existingSignalement.get();
            signalement.setLatitude(firebaseReport.getLat());
            signalement.setLongitude(firebaseReport.getLng());
            signalement.setDescription(firebaseReport.getDescription());
            signalement.setSurfaceM2(firebaseReport.getSurfaceM2());
            signalement.setBudget(firebaseReport.getBudgetEstimated());
            signalement.setUtilisateur(utilisateur);
            signalement.setIdStatus(status.getId());
            signalement.setTypeSignalement(typeSignalement);
            // Ne pas mettre à jour createdAt pour les updates
        } else {
            // INSERT: Créer un nouveau signalement
            signalement = new Signalement();
            signalement.setLatitude(firebaseReport.getLat());
            signalement.setLongitude(firebaseReport.getLng());
            signalement.setDescription(firebaseReport.getDescription());
            signalement.setSurfaceM2(firebaseReport.getSurfaceM2());
            signalement.setBudget(firebaseReport.getBudgetEstimated());
            signalement.setUtilisateur(utilisateur);
            signalement.setIdStatus(status.getId());
            signalement.setTypeSignalement(typeSignalement);
            signalement.setCreatedAt(firebaseReport.getCreatedAt());
        }

        // Toujours mettre à jour le firebase_id
        signalement.setFirebaseId(firebaseReport.getId());

        // Sauvegarder le signalement
        Signalement savedSignalement = signalementService.save(signalement);

        return savedSignalement;
    }

    /**
     * Synchronise les photos pour un signalement dans PostgreSQL
     * UPSERT: UPDATE si existe (par URL), INSERT sinon
     * Supprime les photos non présentes dans Firebase
     */
    public void syncPhotosForReport(String firebaseReportId, Signalement postgresSignalement,
                                     List<FirebasePhotoDTO> firebasePhotos) {
        if (postgresSignalement == null) {
            return;
        }

        // 1. Récupérer les photos existantes pour ce signalement
        List<Photo> existingPhotos = photoRepository.findBySignalementId(postgresSignalement.getId());

        if (firebasePhotos == null || firebasePhotos.isEmpty()) {
            // Si aucune photo Firebase: supprimer toutes les photos existantes
            if (!existingPhotos.isEmpty()) {
                photoRepository.deleteAll(existingPhotos);
            }
            return;
        }

        // 2. Traiter les photos Firebase
        for (FirebasePhotoDTO firebasePhoto : firebasePhotos) {
            try {
                // Chercher si la photo existe déjà par URL
                Optional<Photo> existingPhoto = photoRepository.findByUrl(firebasePhoto.getImgbbUrl());

                Photo photo;
                if (existingPhoto.isPresent() && existingPhoto.get().getSignalement().getId().equals(postgresSignalement.getId())) {
                    // La photo existe pour ce signalement: la mettre à jour
                    photo = existingPhoto.get();
                    photo.setUploadedAt(firebasePhoto.getUploadedAt());
                } else {
                    // Créer une nouvelle photo
                    photo = new Photo();
                    photo.setUrl(firebasePhoto.getImgbbUrl());
                    photo.setSignalement(postgresSignalement);
                    photo.setCreatedAt(LocalDateTime.now());
                    photo.setUploadedAt(firebasePhoto.getUploadedAt());
                }

                photoRepository.save(photo);
            } catch (Exception e) {
                System.err.println("Erreur sauvegarde photo: " + e.getMessage());
            }
        }

        // 3. Supprimer les photos qui n'existent plus dans Firebase
        // (photos orphelines du signalement)
        for (Photo existingPhoto : existingPhotos) {
            boolean foundInFirebase = firebasePhotos.stream()
                    .anyMatch(fp -> fp.getImgbbUrl().equals(existingPhoto.getUrl()));
            if (!foundInFirebase) {
                photoRepository.delete(existingPhoto);
            }
        }
    }

    /**
     * Trouve l'utilisateur par Firebase UID, ou le crée s'il n'existe pas
     */
    private Utilisateur findOrCreateUserByFirebaseUid(String firebaseUid) throws Exception {
        Optional<Utilisateur> user = utilisateurRepository.findByFirebaseUid(firebaseUid);

        if (user.isPresent()) {
            return user.get();
        }

        // Cas logique: l'utilisateur doit exister
        // Si pas trouvé, on throw une exception
        throw new Exception("Utilisateur Firebase UID non trouvé: " + firebaseUid);
    }

    /**
     * Mappe le statut Firebase vers PostgreSQL
     * Normalise la comparaison (en_cours vs En cours)
     */
    private Status mapFirebaseStatus(String firebaseStatus) throws Exception {
        if (firebaseStatus == null || firebaseStatus.isEmpty()) {
            throw new Exception("Statut Firebase vide");
        }

        String normalizedFirebaseStatus = normalizeString(firebaseStatus);

        // Chercher dans PostgreSQL avec normalisation
        List<Status> statuses = statusRepository.findAll();
        for (Status status : statuses) {
            if (normalizeString(status.getLibelle()).equals(normalizedFirebaseStatus)) {
                return status;
            }
        }

        throw new Exception("Statut non trouvé: " + firebaseStatus);
    }

    /**
     * Mappe le type Firebase vers PostgreSQL
     * Normalise la comparaison
     */
    private TypeSignalement mapFirebaseType(String firebaseType) throws Exception {
        if (firebaseType == null || firebaseType.isEmpty()) {
            throw new Exception("Type Firebase vide");
        }

        String normalizedFirebaseType = normalizeString(firebaseType);

        // Chercher dans PostgreSQL avec normalisation
        List<TypeSignalement> types = typeSignalementRepository.findAll();
        for (TypeSignalement type : types) {
            if (normalizeString(type.getLibelle()).equals(normalizedFirebaseType)) {
                return type;
            }
        }

        throw new Exception("Type non trouvé: " + firebaseType);
    }

    /**
     * Normalise une string pour la comparaison
     * - Supprime les accents (é -> e, à -> a, etc)
     * - Trim + lowercase
     * - Remplace underscores par espaces
     * - Supprime espaces multiples
     */
    private String normalizeString(String str) {
        return StringNormalizer.normalize(str);
    }
}
