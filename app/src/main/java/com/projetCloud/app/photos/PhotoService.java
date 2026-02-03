package com.projetCloud.app.photos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service pour gérer les opérations liées aux photos
 */
@Service
public class PhotoService {

    private static final Logger logger = LoggerFactory.getLogger(PhotoService.class);

    @Autowired
    private PhotoRepository photoRepository;

    /**
     * Crée une nouvelle photo
     * @param url URL de la photo
     * @param description Description optionnelle
     * @param fileName Nom du fichier
     * @param mimeType Type MIME
     * @return Photo créée
     */
    public Photo createPhoto(String url, String description, String fileName, String mimeType) {
        logger.info("Création d'une nouvelle photo: {}", fileName);
        
        Photo photo = new Photo(url, description, fileName, mimeType);
        Photo saved = photoRepository.save(photo);
        
        logger.info("Photo créée avec succès - ID: {}", saved.getId());
        return saved;
    }

    /**
     * Crée une photo avec informations complètes
     * @param url URL de la photo
     * @param description Description
     * @param fileName Nom du fichier  
     * @param fileSize Taille du fichier
     * @param mimeType Type MIME
     * @return Photo créée
     */
    public Photo createPhoto(String url, String description, String fileName, Long fileSize, String mimeType) {
        logger.info("Création d'une nouvelle photo avec taille: {} - {} bytes", fileName, fileSize);
        
        Photo photo = new Photo(url, description, fileName, mimeType);
        photo.setFileSize(fileSize);
        
        Photo saved = photoRepository.save(photo);
        logger.info("Photo créée avec succès - ID: {}", saved.getId());
        return saved;
    }

    /**
     * Trouve une photo par son ID
     * @param id ID de la photo
     * @return Photo optionnelle
     */
    public Optional<Photo> findById(Long id) {
        return photoRepository.findById(id);
    }

    /**
     * Trouve une photo par son URL
     * @param url URL de la photo
     * @return Photo optionnelle
     */
    public Optional<Photo> findByUrl(String url) {
        return photoRepository.findByUrl(url);
    }

    /**
     * Trouve ou crée une photo par URL
     * @param url URL de la photo
     * @param fileName Nom du fichier (optionnel)
     * @param mimeType Type MIME (optionnel)
     * @return Photo trouvée ou créée
     */
    public Photo findOrCreateByUrl(String url, String fileName, String mimeType) {
        Optional<Photo> existingPhoto = photoRepository.findByUrl(url);
        
        if (existingPhoto.isPresent()) {
            logger.info("Photo trouvée avec URL: {}", url);
            return existingPhoto.get();
        } else {
            logger.info("Création d'une nouvelle photo avec URL: {}", url);
            return createPhoto(url, null, fileName, mimeType);
        }
    }

    /**
     * Met à jour une photo existante
     * @param id ID de la photo
     * @param url Nouvelle URL
     * @param description Nouvelle description
     * @return Photo mise à jour ou null si non trouvée
     */
    public Photo updatePhoto(Long id, String url, String description) {
        Optional<Photo> photoOpt = photoRepository.findById(id);
        
        if (photoOpt.isPresent()) {
            Photo photo = photoOpt.get();
            if (url != null) photo.setUrl(url);
            if (description != null) photo.setDescription(description);
            
            Photo saved = photoRepository.save(photo);
            logger.info("Photo mise à jour - ID: {}", saved.getId());
            return saved;
        }
        
        logger.warn("Photo non trouvée pour mise à jour - ID: {}", id);
        return null;
    }

    /**
     * Supprime une photo par son ID
     * @param id ID de la photo
     * @return true si supprimée, false si non trouvée
     */
    public boolean deletePhoto(Long id) {
        if (photoRepository.existsById(id)) {
            photoRepository.deleteById(id);
            logger.info("Photo supprimée - ID: {}", id);
            return true;
        } else {
            logger.warn("Photo non trouvée pour suppression - ID: {}", id);
            return false;
        }
    }

    /**
     * Récupère toutes les photos
     * @return Liste de toutes les photos
     */
    public List<Photo> findAll() {
        return photoRepository.findAll();
    }

    /**
     * Recherche des photos par description
     * @param keyword Mot-clé à rechercher
     * @return Liste des photos correspondantes
     */
    public List<Photo> searchByDescription(String keyword) {
        return photoRepository.findByDescriptionContaining(keyword);
    }

    /**
     * Récupère les statistiques des photos
     * @return Nombre total de photos
     */
    public long getTotalPhotoCount() {
        return photoRepository.count();
    }

    /**
     * Récupère le nombre de photos par type MIME
     * @param mimeType Type MIME
     * @return Nombre de photos
     */
    public long getPhotoCountByMimeType(String mimeType) {
        return photoRepository.countByMimeType(mimeType);
    }

    /**
     * Récupère toutes les photos d'un signalement
     * @param signalementId ID du signalement
     * @return Liste des photos du signalement
     */
    public List<Photo> getPhotosBySignalement(Long signalementId) {
        return photoRepository.findBySignalementId(signalementId);
    }

    /**
     * Compte le nombre de photos d'un signalement
     * @param signalementId ID du signalement
     * @return Nombre de photos
     */
    public long getPhotoCountBySignalement(Long signalementId) {
        return photoRepository.countBySignalementId(signalementId);
    }

    /**
     * Supprime toutes les photos d'un signalement
     * @param signalementId ID du signalement
     * @return Nombre de photos supprimées
     */
    public long deletePhotosBySignalement(Long signalementId) {
        List<Photo> photos = photoRepository.findBySignalementId(signalementId);
        int count = photos.size();
        photoRepository.deleteAll(photos);
        logger.info("Supprimé {} photos pour le signalement ID: {}", count, signalementId);
        return count;
    }
}