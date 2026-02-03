package com.projetCloud.app.photos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    /**
     * Trouve une photo par son URL
     * @param url URL de la photo
     * @return Photo optionnelle
     */
    Optional<Photo> findByUrl(String url);

    /**
     * Trouve toutes les photos par nom de fichier
     * @param fileName nom du fichier
     * @return Liste des photos
     */
    List<Photo> findByFileName(String fileName);

    /**
     * Trouve toutes les photos par type MIME
     * @param mimeType type MIME
     * @return Liste des photos
     */
    List<Photo> findByMimeType(String mimeType);

    /**
     * Trouve toutes les photos contenant un mot-clé dans la description
     * @param keyword mot-clé à rechercher
     * @return Liste des photos
     */
    @Query("SELECT p FROM Photo p WHERE p.description LIKE %:keyword%")
    List<Photo> findByDescriptionContaining(@Param("keyword") String keyword);

    /**
     * Trouve toutes les photos avec un nom de fichier contenant un motif
     * @param pattern motif à rechercher
     * @return Liste des photos
     */
    @Query("SELECT p FROM Photo p WHERE p.fileName LIKE %:pattern%")
    List<Photo> findByFileNameContaining(@Param("pattern") String pattern);

    /**
     * Compte le nombre de photos par type MIME
     * @param mimeType type MIME
     * @return Nombre de photos
     */
    long countByMimeType(String mimeType);

    /**
     * Supprime toutes les photos avec un URL donné
     * @param url URL de la photo
     */
    void deleteByUrl(String url);

    /**
     * Trouve toutes les photos d'un signalement
     * @param signalementId ID du signalement
     * @return Liste des photos
     */
    @Query("SELECT p FROM Photo p WHERE p.signalement.id = :signalementId ORDER BY p.createdAt")
    List<Photo> findBySignalementId(@Param("signalementId") Long signalementId);

    /**
     * Compte le nombre de photos d'un signalement
     * @param signalementId ID du signalement
     * @return Nombre de photos
     */
    @Query("SELECT COUNT(p) FROM Photo p WHERE p.signalement.id = :signalementId")
    long countBySignalementId(@Param("signalementId") Long signalementId);

    /**
     * Trouve les signalements ayant au moins une photo
     * @return Liste des IDs de signalements
     */
    @Query("SELECT DISTINCT p.signalement.id FROM Photo p")
    List<Long> findSignalementsWithPhotos();
}