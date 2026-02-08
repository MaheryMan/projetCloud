package com.projetCloud.app.sync;

import com.projetCloud.app.photos.Photo;
import com.projetCloud.app.photos.PhotoRepository;
import com.projetCloud.app.service.FileStorageService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service pour synchroniser les images depuis imgbb vers le stockage local
 */
@Service
public class ImageSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ImageSyncService.class);

    @Autowired
    private PhotoRepository photoRepository;

    @Value("${file.upload-dir:uploads/photos}")
    private String uploadDir;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Synchronise toutes les images imgbb vers le stockage local
     * - Récupère toutes les photos avec URL imgbb
     * - Télécharge celles qui ne sont pas encore en local
     * - Met à jour la BDD avec le chemin local
     * 
     * @return Résultat de la synchronisation
     */
    public ImageSyncResult syncImagesFromImgbb() {
        logger.info("🔄 Démarrage de la synchronisation des images depuis imgbb...");
        
        ImageSyncResult result = new ImageSyncResult();
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            // Créer le répertoire si nécessaire
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            logger.error("❌ Impossible de créer le répertoire de stockage", e);
            result.setError("Impossible de créer le répertoire: " + e.getMessage());
            return result;
        }

        // Récupérer toutes les photos avec URL imgbb
        List<Photo> photos = photoRepository.findAll();
        List<Photo> imgbbPhotos = new ArrayList<>();
        
        for (Photo photo : photos) {
            if (photo.getUrl() != null && photo.getUrl().contains("imgbb.com")) {
                imgbbPhotos.add(photo);
            }
        }

        logger.info("📊 {} photos imgbb trouvées dans la base", imgbbPhotos.size());
        result.setTotalFound(imgbbPhotos.size());

        String baseUrl = "http://localhost:" + serverPort + "/uploads/photos/";

        // Traiter chaque photo
        for (Photo photo : imgbbPhotos) {
            try {
                // Vérifier si le fichier existe déjà localement
                if (photo.getFileName() != null && !photo.getFileName().isEmpty()) {
                    Path localFile = uploadPath.resolve(photo.getFileName());
                    if (Files.exists(localFile)) {
                        logger.debug("⏭️  Image déjà locale: {}", photo.getFileName());
                        result.incrementSkipped();
                        continue;
                    }
                }

                // Télécharger l'image depuis imgbb
                logger.info("⬇️  Téléchargement: {}", photo.getUrl());
                
                URL url = new URL(photo.getUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    logger.warn("⚠️  Erreur HTTP {} pour: {}", responseCode, photo.getUrl());
                    result.incrementFailed();
                    result.addError("HTTP " + responseCode + " pour photo ID " + photo.getId());
                    continue;
                }

                // Déterminer l'extension du fichier
                String contentType = connection.getContentType();
                String fileExtension = getExtensionFromContentType(contentType);
                if (fileExtension == null) {
                    fileExtension = getExtensionFromUrl(photo.getUrl());
                }

                // Générer un nom de fichier unique
                String fileName = UUID.randomUUID().toString() + fileExtension;
                Path targetPath = uploadPath.resolve(fileName);

                // Copier le contenu vers le fichier local
                try (InputStream inputStream = connection.getInputStream()) {
                    long fileSize = Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    
                    // Mettre à jour la photo dans la BDD
                    photo.setFileName(fileName);
                    photo.setFileSize(fileSize);
                    photo.setMimeType(contentType);
                    photo.setUpdatedAt(LocalDateTime.now());
                    
                    photoRepository.save(photo);
                    
                    logger.info("✅ Image téléchargée: {} ({} bytes)", fileName, fileSize);
                    result.incrementDownloaded();
                }

            } catch (IOException e) {
                logger.error("❌ Erreur lors du téléchargement de l'image ID {}: {}", photo.getId(), e.getMessage());
                result.incrementFailed();
                result.addError("Photo ID " + photo.getId() + ": " + e.getMessage());
            } catch (Exception e) {
                logger.error("❌ Erreur inattendue pour photo ID {}: {}", photo.getId(), e.getMessage());
                result.incrementFailed();
                result.addError("Photo ID " + photo.getId() + ": " + e.getMessage());
            }
        }

        logger.info("✨ Synchronisation terminée: {} téléchargées, {} ignorées, {} échouées", 
                    result.getDownloaded(), result.getSkipped(), result.getFailed());
        
        return result;
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

    /**
     * Classe pour retourner le résultat de la synchronisation
     */
    public static class ImageSyncResult {
        private int totalFound = 0;
        private int downloaded = 0;
        private int skipped = 0;
        private int failed = 0;
        private List<String> errors = new ArrayList<>();
        private String error = null;

        public int getTotalFound() { return totalFound; }
        public void setTotalFound(int totalFound) { this.totalFound = totalFound; }

        public int getDownloaded() { return downloaded; }
        public void incrementDownloaded() { this.downloaded++; }

        public int getSkipped() { return skipped; }
        public void incrementSkipped() { this.skipped++; }

        public int getFailed() { return failed; }
        public void incrementFailed() { this.failed++; }

        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String getMessage() {
            if (error != null) return error;
            return String.format("Synchronisation terminée: %d téléchargées, %d ignorées, %d échouées sur %d trouvées",
                    downloaded, skipped, failed, totalFound);
        }
    }
}
