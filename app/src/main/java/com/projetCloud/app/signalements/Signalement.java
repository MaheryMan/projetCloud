package com.projetCloud.app.signalements;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projetCloud.app.typesSignalement.TypeSignalement;
import com.projetCloud.app.utilisateurs.Utilisateur;
import com.projetCloud.app.photos.Photo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "signalements")
public class Signalement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 10, scale = 6, nullable = false)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6, nullable = false)
    private BigDecimal longitude;

    @Column(name = "surface_m2", precision = 15, scale = 2)
    private BigDecimal surfaceM2;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 15, scale = 2)
    private BigDecimal budget;

    @Column(name = "niveau")
    private Integer niveau;

    @Column(name = "id_entreprise")
    private Long idEntreprise;

    @Column(name = "id_status", nullable = false)
    private Long idStatus;

   

    @ManyToOne
    @JoinColumn(name = "id_type_signalement", nullable = false)
    private TypeSignalement typeSignalement;

    @ManyToOne
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    // Relation avec Photos (liste)
    @OneToMany(mappedBy = "signalement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Photo> photos = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Champs de synchronisation Firebase
    @Column(name = "is_synced_to_firebase")
    private Boolean isSyncedToFirebase = false;

    @Column(name = "firebase_id", length = 128)
    private String firebaseId;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Column(name = "synced_from_firebase_at")
    private LocalDateTime syncedFromFirebaseAt;

    @Column(name = "needs_firebase_sync")
    private Boolean needsFirebaseSync = false;
    // Champ transient pour la date de dernière mise à jour depuis historique
    @Transient
    @JsonProperty("lastHistoriqueDate")
    private LocalDateTime lastHistoriqueDate;

    // Constructeurs
    public Signalement() {}

    public Signalement(BigDecimal latitude, BigDecimal longitude, BigDecimal surfaceM2, BigDecimal budget, Integer niveau, String description, Long idEntreprise, Long idStatus, TypeSignalement typeSignalement, Utilisateur utilisateur) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.surfaceM2 = surfaceM2;
        this.budget = budget;
        this.niveau = niveau;
        this.description = description;
        this.idEntreprise = idEntreprise;
        this.idStatus = idStatus;
        this.typeSignalement = typeSignalement;
        this.utilisateur = utilisateur;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getSurfaceM2() {
        return surfaceM2;
    }

    public void setSurfaceM2(BigDecimal surfaceM2) {
        this.surfaceM2 = surfaceM2;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }
    public Long getIdEntreprise() {
        return idEntreprise;
    }

    public void setIdEntreprise(Long idEntreprise) {
        this.idEntreprise = idEntreprise;
    }

    public Long getIdStatus() {
        return idStatus;
    }

    public void setIdStatus(Long idStatus) {
        this.idStatus = idStatus;
    }

    public Integer getNiveau() {
        return niveau;
    }

    public void setNiveau(Integer niveau) {
        this.niveau = niveau;
    }

    public TypeSignalement getTypeSignalement() {
        return typeSignalement;
    }

    public void setTypeSignalement(TypeSignalement typeSignalement) {
        this.typeSignalement = typeSignalement;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsSyncedToFirebase() {
        return isSyncedToFirebase;
    }

    public void setIsSyncedToFirebase(Boolean isSyncedToFirebase) {
        this.isSyncedToFirebase = isSyncedToFirebase;
    }

    public String getFirebaseId() {
        return firebaseId;
    }

    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }

    public LocalDateTime getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(LocalDateTime syncedAt) {
        this.syncedAt = syncedAt;
    }

    public LocalDateTime getSyncedFromFirebaseAt() {
        return syncedFromFirebaseAt;
    }

    public void setSyncedFromFirebaseAt(LocalDateTime syncedFromFirebaseAt) {
        this.syncedFromFirebaseAt = syncedFromFirebaseAt;
    }

    public Boolean getNeedsFirebaseSync() {
        return needsFirebaseSync;
    }

    public void setNeedsFirebaseSync(Boolean needsFirebaseSync) {
        this.needsFirebaseSync = needsFirebaseSync;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }

    /**
     * Ajoute une photo au signalement
     * @param photo Photo à ajouter
     */
    public void addPhoto(Photo photo) {
        photos.add(photo);
        photo.setSignalement(this);
    }

    /**
     * Supprime une photo du signalement
     * @param photo Photo à supprimer
     */
    public void removePhoto(Photo photo) {
        photos.remove(photo);
        photo.setSignalement(null);
    }

    /**
     * Méthode utilitaire pour obtenir l'URL de la première photo
     * @return URL de la première photo ou null si pas de photos
     */
    public String getPhotoUrl() {
        return photos.isEmpty() ? null : photos.get(0).getUrl();
    }

    /**
     * Méthode utilitaire pour obtenir toutes les URLs des photos
     * @return Liste des URLs des photos
     */
    public List<String> getPhotoUrls() {
        return photos.stream().map(Photo::getUrl).toList();
    }
    public LocalDateTime getLastHistoriqueDate() {
        return lastHistoriqueDate;
    }

    public void setLastHistoriqueDate(LocalDateTime lastHistoriqueDate) {
        this.lastHistoriqueDate = lastHistoriqueDate;
    }

    // Méthode appelée automatiquement avant l'insertion en base
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    // Méthode appelée automatiquement avant la mise à jour en base
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}