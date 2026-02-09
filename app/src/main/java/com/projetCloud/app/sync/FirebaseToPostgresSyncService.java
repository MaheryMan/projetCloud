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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service pour synchroniser les données Firebase vers PostgreSQL
 */
@Service
public class FirebaseToPostgresSyncService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseToPostgresSyncService.class);

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

    @Value("${file.upload-dir:uploads/photos}")
    private String uploadDir;

    @Value("${server.port:8080}")
    private String serverPort;

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
     * STRATÉGIE: Supprimer TOUS les photos existantes et recharger depuis Firebase
     * Cela évite les incohérences URLs (local vs imgbb)
     */
    public void syncPhotosForReport(String firebaseReportId, Signalement postgresSignalement,
                                     List<FirebasePhotoDTO> firebasePhotos) {
        if (postgresSignalement == null) {
            System.out.println("[SYNC SERVICE] ⚠️ Signalement PostgreSQL NULL pour report Firebase: " + firebaseReportId);
            return;
        }

        System.out.println("[SYNC SERVICE] 🔄 Sync photos pour signalement PostgreSQL ID=" + postgresSignalement.getId() + 
            " (Firebase report ID=" + firebaseReportId + ")");
        System.out.println("[SYNC SERVICE] 📊 Photos Firebase à synchroniser: " + (firebasePhotos != null ? firebasePhotos.size() : "NULL"));

        // 1. SUPPRIMER TOUTES les photos existantes pour ce signalement
        // (Évite les incohérences URLs: URLs locales vs URLs imgbb)
        List<Photo> existingPhotos = photoRepository.findBySignalementId(postgresSignalement.getId());
        if (!existingPhotos.isEmpty()) {
            System.out.println("[SYNC SERVICE] 🗑️ SUPPRESSION de TOUTES les photos existantes: " + existingPhotos.size() + 
                " (pour éviter incohérences URL)");
            photoRepository.deleteAll(existingPhotos);
            System.out.println("[SYNC SERVICE] ✅ Toutes les photos supprimées");
        }

        // 2. Si aucune photo Firebase: fin
        if (firebasePhotos == null || firebasePhotos.isEmpty()) {
            System.out.println("[SYNC SERVICE] 📊 Aucune photo Firebase à synchroniser pour ce signalement");
            return;
        }

        // 3. Télécharger et créer toutes les photos Firebase
        int photosDownloaded = 0;
        int photosFailed = 0;
        
        for (FirebasePhotoDTO firebasePhoto : firebasePhotos) {
            try {
                String imgbbUrl = firebasePhoto.getImgbbUrl();
                System.out.println("[SYNC SERVICE] ⬇️ Téléchargement photo: " + imgbbUrl.substring(0, Math.min(30, imgbbUrl.length())) + "...");
                
                // Télécharger l'image depuis imgbb et créer une nouvelle photo
                Photo photo = downloadAndSavePhoto(firebasePhoto, postgresSignalement);
                if (photo != null) {
                    photoRepository.save(photo);
                    photosDownloaded++;
                    System.out.println("[SYNC SERVICE] ✅ Nouvelle photo téléchargée: " + photo.getFileName());
                } else {
                    photosFailed++;
                    System.out.println("[SYNC SERVICE] ⚠️ Erreur téléchargement photo (retourné null)");
                }
            } catch (Exception e) {
                photosFailed++;
                System.err.println("[SYNC SERVICE] ❌ Erreur sauvegarde photo pour signalement " + postgresSignalement.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("[SYNC SERVICE] 📊 Résumé sync photos: downloaded=" + photosDownloaded + " failed=" + photosFailed + 
            " total=" + firebasePhotos.size());
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

    /**
     * Cherche une photo existante par URL imgbb (dans URL ou description)
     * @param imgbbUrl URL imgbb à chercher
     * @param signalementId ID du signalement
     * @return Photo existante ou empty
     */
    private Optional<Photo> findExistingPhotoByImgbbUrl(String imgbbUrl, Long signalementId) {
        // D'abord chercher par URL directe ET signalement (évite les doublons)
        Optional<Photo> photoByUrl = photoRepository.findByUrlAndSignalementId(imgbbUrl, signalementId);
        if (photoByUrl.isPresent()) {
            return photoByUrl;
        }

        // Ensuite chercher dans les descriptions (cas nouveau : stocke imgbb en description)
        List<Photo> allPhotos = photoRepository.findBySignalementId(signalementId);
        for (Photo photo : allPhotos) {
            if (photo.getDescription() != null && photo.getDescription().contains(imgbbUrl)) {
                return Optional.of(photo);
            }
        }

        return Optional.empty();
    }

    /**
     * Télécharge une image depuis imgbb et la sauvegarde localement
     * @param firebasePhoto DTO de la photo Firebase
     * @param signalement Signalement associé
     * @return Photo créée ou null en cas d'erreur
     */
    private Photo downloadAndSavePhoto(FirebasePhotoDTO firebasePhoto, Signalement signalement) {
        String imgbbUrl = firebasePhoto.getImgbbUrl();
        
        if (imgbbUrl == null || imgbbUrl.isEmpty()) {
            logger.warn("URL imgbb vide pour la photo du signalement {}", signalement.getId());
            return null;
        }

        try {
            // Créer le répertoire si nécessaire
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            logger.info("⬇️  Téléchargement de l'image depuis imgbb: {}", imgbbUrl);

            // Télécharger l'image
            URL url = new URL(imgbbUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.warn("⚠️  Erreur HTTP {} lors du téléchargement de: {}", responseCode, imgbbUrl);
                return null;
            }

            // Déterminer l'extension du fichier
            String contentType = connection.getContentType();
            String fileExtension = getExtensionFromContentType(contentType);
            if (fileExtension == null) {
                fileExtension = getExtensionFromUrl(imgbbUrl);
            }

            // Générer un nom de fichier unique
            String fileName = UUID.randomUUID().toString() + fileExtension;
            Path targetPath = uploadPath.resolve(fileName);

            // Copier le contenu vers le fichier local
            long fileSize;
            try (InputStream inputStream = connection.getInputStream()) {
                fileSize = Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Créer l'URL locale
            String localUrl = "http://localhost:" + serverPort + "/uploads/photos/" + fileName;

            // Créer l'entité Photo
            Photo photo = new Photo();
            photo.setUrl(localUrl);  // URL locale au lieu de imgbb
            photo.setFileName(fileName);
            photo.setFileSize(fileSize);
            photo.setMimeType(contentType);
            photo.setSignalement(signalement);
            photo.setCreatedAt(LocalDateTime.now());
            photo.setUploadedAt(firebasePhoto.getUploadedAt());
            photo.setDescription("Téléchargée depuis imgbb: " + imgbbUrl);

            logger.info("✅ Image téléchargée et sauvegardée: {} ({} bytes)", fileName, fileSize);
            return photo;

        } catch (IOException e) {
            logger.error("❌ Erreur lors du téléchargement de l'image depuis imgbb: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("❌ Erreur inattendue lors du traitement de la photo: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrait l'extension depuis le Content-Type
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) return ".jpg";
        
        if (contentType.contains("jpeg") || contentType.contains("jpg")) return ".jpg";
        if (contentType.contains("png")) return ".png";
        if (contentType.contains("gif")) return ".gif";
        if (contentType.contains("webp")) return ".webp";
        
        return ".jpg"; // Par défaut
    }

    /**
     * Extrait l'extension depuis l'URL
     */
    private String getExtensionFromUrl(String url) {
        if (url == null) return ".jpg";
        
        // Extraire l'extension de l'URL
        int lastDot = url.lastIndexOf('.');
        int lastSlash = url.lastIndexOf('/');
        int queryStart = url.indexOf('?');
        
        if (lastDot > lastSlash && lastDot > 0) {
            int endIndex = queryStart > 0 ? queryStart : url.length();
            String ext = url.substring(lastDot, Math.min(lastDot + 5, endIndex));
            if (ext.matches("\\.[a-zA-Z]{3,4}")) {
                return ext.toLowerCase();
            }
        }
        
        return ".jpg"; // Par défaut
    }
}
