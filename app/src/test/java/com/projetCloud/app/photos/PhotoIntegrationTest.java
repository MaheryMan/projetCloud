package com.projetCloud.app.photos;

import com.projetCloud.app.signalements.Signalement;
import com.projetCloud.app.signalements.SignalementRepository;
import com.projetCloud.app.typesSignalement.TypeSignalement;
import com.projetCloud.app.typesSignalement.TypeSignalementRepository;
import com.projetCloud.app.utilisateurs.Utilisateur;
import com.projetCloud.app.utilisateurs.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour l'entité Photo
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class PhotoIntegrationTest {

    @Autowired
    private PhotoService photoService;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private SignalementRepository signalementRepository;

    @Autowired
    private TypeSignalementRepository typeSignalementRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Test
    public void testCreatePhoto() {
        // Given
        String url = "https://example.com/test-photo.jpg";
        String description = "Photo de test";
        String fileName = "test-photo.jpg";
        String mimeType = "image/jpeg";

        // When
        Photo photo = photoService.createPhoto(url, description, fileName, mimeType);

        // Then
        assertNotNull(photo.getId());
        assertEquals(url, photo.getUrl());
        assertEquals(description, photo.getDescription());
        assertEquals(fileName, photo.getFileName());
        assertEquals(mimeType, photo.getMimeType());
        assertNotNull(photo.getCreatedAt());
        assertNotNull(photo.getUploadedAt());
    }

    @Test
    public void testFindOrCreateByUrl() {
        // Given
        String url = "https://example.com/unique-photo.jpg";
        String fileName = "unique-photo.jpg";
        String mimeType = "image/jpeg";

        // When - Première création
        Photo photo1 = photoService.findOrCreateByUrl(url, fileName, mimeType);
        Long photoId1 = photo1.getId();

        // When - Deuxième appel avec la même URL
        Photo photo2 = photoService.findOrCreateByUrl(url, fileName, mimeType);
        Long photoId2 = photo2.getId();

        // Then - Devrait retourner la même photo
        assertEquals(photoId1, photoId2);
        assertEquals(url, photo2.getUrl());
    }

    @Test
    public void testUpdatePhoto() {
        // Given
        String originalUrl = "https://example.com/original.jpg";
        Photo photo = photoService.createPhoto(originalUrl, "Original", "original.jpg", "image/jpeg");
        Long photoId = photo.getId();

        // When
        String newUrl = "https://example.com/updated.jpg";
        String newDescription = "Updated description";
        Photo updatedPhoto = photoService.updatePhoto(photoId, newUrl, newDescription);

        // Then
        assertNotNull(updatedPhoto);
        assertEquals(newUrl, updatedPhoto.getUrl());
        assertEquals(newDescription, updatedPhoto.getDescription());
        assertEquals(photoId, updatedPhoto.getId());
    }

    @Test
    public void testDeletePhoto() {
        // Given
        String url = "https://example.com/to-delete.jpg";
        Photo photo = photoService.createPhoto(url, "To delete", "delete.jpg", "image/jpeg");
        Long photoId = photo.getId();

        // When
        boolean deleted = photoService.deletePhoto(photoId);

        // Then
        assertTrue(deleted);
        assertFalse(photoRepository.existsById(photoId));
    }

    @Test
    public void testSearchByDescription() {
        // Given
        photoService.createPhoto("https://example.com/search1.jpg", "Voiture rouge", "search1.jpg", "image/jpeg");
        photoService.createPhoto("https://example.com/search2.jpg", "Voiture bleue", "search2.jpg", "image/jpeg");
        photoService.createPhoto("https://example.com/search3.jpg", "Maison rouge", "search3.jpg", "image/jpeg");

        // When
        var photosWithRouge = photoService.searchByDescription("rouge");
        var photosWithVoiture = photoService.searchByDescription("Voiture");

        // Then
        assertEquals(2, photosWithRouge.size()); // Voiture rouge + Maison rouge
        assertEquals(2, photosWithVoiture.size()); // Voiture rouge + Voiture bleue
    }

    @Test
    public void testMultiplePhotosPerSignalement() {
        // Given - Créer un signalement de test
        Signalement signalement = createTestSignalement();
        
        // When - Ajouter plusieurs photos
        Photo photo1 = photoService.createPhoto("https://example.com/angle1.jpg", "Vue d'angle 1", "angle1.jpg", "image/jpeg");
        Photo photo2 = photoService.createPhoto("https://example.com/angle2.jpg", "Vue d'angle 2", "angle2.jpg", "image/jpeg");
        Photo photo3 = photoService.createPhoto("https://example.com/detail.jpg", "Détail du problème", "detail.jpg", "image/jpeg");
        
        signalement.addPhoto(photo1);
        signalement.addPhoto(photo2);
        signalement.addPhoto(photo3);
        
        Signalement savedSignalement = signalementRepository.save(signalement);
        
        // Then
        assertEquals(3, savedSignalement.getPhotos().size());
        assertEquals("https://example.com/angle1.jpg", savedSignalement.getPhotoUrl()); // Première photo
        
        List<String> photoUrls = savedSignalement.getPhotoUrls();
        assertEquals(3, photoUrls.size());
        assertTrue(photoUrls.contains("https://example.com/angle1.jpg"));
        assertTrue(photoUrls.contains("https://example.com/angle2.jpg"));
        assertTrue(photoUrls.contains("https://example.com/detail.jpg"));
    }

    @Test
    public void testGetPhotosBySignalement() {
        // Given
        Signalement signalement = createTestSignalement();
        
        Photo photo1 = photoService.createPhoto("https://example.com/test1.jpg", "Test 1", "test1.jpg", "image/jpeg");
        Photo photo2 = photoService.createPhoto("https://example.com/test2.jpg", "Test 2", "test2.jpg", "image/jpeg");
        
        signalement.addPhoto(photo1);
        signalement.addPhoto(photo2);
        signalementRepository.save(signalement);
        
        // When
        List<Photo> photos = photoService.getPhotosBySignalement(signalement.getId());
        long count = photoService.getPhotoCountBySignalement(signalement.getId());
        
        // Then
        assertEquals(2, photos.size());
        assertEquals(2, count);
    }

    private Signalement createTestSignalement() {
        // Cette méthode peut nécessiter des ajustements selon votre configuration de test
        // Pour l'instant, retourne un signalement basique
        Signalement signalement = new Signalement();
        signalement.setLatitude(new BigDecimal("-18.9095"));
        signalement.setLongitude(new BigDecimal("47.5256"));
        signalement.setDescription("Test signalement");
        signalement.setIdStatus(1L);
        return signalementRepository.save(signalement);
    }
}