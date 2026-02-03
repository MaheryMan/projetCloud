package com.projetCloud.app.photos;

import com.projetCloud.app.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur REST pour gérer les photos
 */
@RestController
@RequestMapping("/api/photos")
@CrossOrigin(origins = "*")
public class PhotoController {

    @Autowired
    private PhotoService photoService;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Upload de fichiers photos et création des entités Photo
     * Retourne les URLs des fichiers uploadés
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadPhotos(@RequestParam("files") MultipartFile[] files) {
        try {
            List<String> photoUrls = new ArrayList<>();
            
            for (MultipartFile file : files) {
                // Vérifier que le fichier est une image
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest()
                        .body("Le fichier " + file.getOriginalFilename() + " n'est pas une image");
                }
                
                // Sauvegarder le fichier et obtenir l'URL
                String fileUrl = fileStorageService.storeFile(file);
                photoUrls.add(fileUrl);
            }
            
            return ResponseEntity.ok(photoUrls);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de l'upload des photos: " + e.getMessage());
        }
    }

    /**
     * Récupère toutes les photos
     */
    @GetMapping
    public ResponseEntity<List<Photo>> getAllPhotos() {
        List<Photo> photos = photoService.findAll();
        return ResponseEntity.ok(photos);
    }

    /**
     * Récupère une photo par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Photo> getPhotoById(@PathVariable Long id) {
        Optional<Photo> photo = photoService.findById(id);
        return photo.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Recherche une photo par URL
     */
    @GetMapping("/by-url")
    public ResponseEntity<Photo> getPhotoByUrl(@RequestParam String url) {
        Optional<Photo> photo = photoService.findByUrl(url);
        return photo.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crée une nouvelle photo
     */
    @PostMapping
    public ResponseEntity<Photo> createPhoto(@RequestBody PhotoRequest request) {
        try {
            Photo photo;
            if (request.getFileSize() != null) {
                photo = photoService.createPhoto(
                    request.getUrl(),
                    request.getDescription(),
                    request.getFileName(),
                    request.getFileSize(),
                    request.getMimeType()
                );
            } else {
                photo = photoService.createPhoto(
                    request.getUrl(),
                    request.getDescription(),
                    request.getFileName(),
                    request.getMimeType()
                );
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(photo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Met à jour une photo existante
     */
    @PutMapping("/{id}")
    public ResponseEntity<Photo> updatePhoto(
            @PathVariable Long id,
            @RequestBody PhotoUpdateRequest request) {
        try {
            Photo updated = photoService.updatePhoto(id, request.getUrl(), request.getDescription());
            if (updated != null) {
                return ResponseEntity.ok(updated);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Supprime une photo
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id) {
        boolean deleted = photoService.deletePhoto(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Recherche des photos par description
     */
    @GetMapping("/search")
    public ResponseEntity<List<Photo>> searchPhotos(@RequestParam String keyword) {
        List<Photo> photos = photoService.searchByDescription(keyword);
        return ResponseEntity.ok(photos);
    }

    /**
     * Récupère les statistiques des photos
     */
    @GetMapping("/stats")
    public ResponseEntity<PhotoStats> getPhotoStats() {
        PhotoStats stats = new PhotoStats();
        stats.setTotalPhotos(photoService.getTotalPhotoCount());
        stats.setImageCount(photoService.getPhotoCountByMimeType("image"));
        return ResponseEntity.ok(stats);
    }

    /**
     * Récupère les photos d'un signalement spécifique
     */
    @GetMapping("/signalement/{signalementId}")
    public ResponseEntity<List<Photo>> getPhotosBySignalement(@PathVariable Long signalementId) {
        List<Photo> photos = photoService.getPhotosBySignalement(signalementId);
        return ResponseEntity.ok(photos);
    }

    /**
     * Compte le nombre de photos d'un signalement
     */
    @GetMapping("/signalement/{signalementId}/count")
    public ResponseEntity<Long> getPhotoCountBySignalement(@PathVariable Long signalementId) {
        long count = photoService.getPhotoCountBySignalement(signalementId);
        return ResponseEntity.ok(count);
    }

    // Classes pour les requêtes
    public static class PhotoRequest {
        private String url;
        private String description;
        private String fileName;
        private Long fileSize;
        private String mimeType;

        // Getters et Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    }

    public static class PhotoUpdateRequest {
        private String url;
        private String description;

        // Getters et Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class PhotoStats {
        private long totalPhotos;
        private long imageCount;

        // Getters et Setters
        public long getTotalPhotos() { return totalPhotos; }
        public void setTotalPhotos(long totalPhotos) { this.totalPhotos = totalPhotos; }

        public long getImageCount() { return imageCount; }
        public void setImageCount(long imageCount) { this.imageCount = imageCount; }
    }
}