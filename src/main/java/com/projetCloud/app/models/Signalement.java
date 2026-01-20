package com.projetCloud.app.models;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "signalements")
public class Signalement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, precision = 20, scale = 10)
    private BigDecimal latitude;

    @Column(precision = 17, scale = 2)
    private BigDecimal surfaceM2;

    @Column(nullable = false, precision = 20, scale = 10)
    private BigDecimal longitude;

    private LocalDateTime dateSignalement;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "id_niveau_travaux", nullable = false)
    private Integer idNiveauTravaux;

    @Column(name = "id_utilisateur", nullable = false)
    private Integer idUtilisateur;

    public Signalement() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getSurfaceM2() {
        return surfaceM2;
    }

    public void setSurfaceM2(BigDecimal surfaceM2) {
        this.surfaceM2 = surfaceM2;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getDateSignalement() {
        return dateSignalement;
    }

    public void setDateSignalement(LocalDateTime dateSignalement) {
        this.dateSignalement = dateSignalement;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getIdNiveauTravaux() {
        return idNiveauTravaux;
    }

    public void setIdNiveauTravaux(Integer idNiveauTravaux) {
        this.idNiveauTravaux = idNiveauTravaux;
    }

    public Integer getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(Integer idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }
}
