package com.projetCloud.app.prixForfaitaire;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prix_forfaitaire")
public class PrixForfaitaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prix_par_metre_carre", precision = 15, scale = 2, nullable = false)
    private BigDecimal prixParMetreCarre;

    @Column(name = "multiplicateur_niveau", precision = 5, scale = 2, nullable = false)
    private BigDecimal multiplicateurNiveau;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Constructeurs
    public PrixForfaitaire() {}

    public PrixForfaitaire(BigDecimal prixParMetreCarre, BigDecimal multiplicateurNiveau) {
        this.prixParMetreCarre = prixParMetreCarre;
        this.multiplicateurNiveau = multiplicateurNiveau;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getPrixParMetreCarre() {
        return prixParMetreCarre;
    }

    public void setPrixParMetreCarre(BigDecimal prixParMetreCarre) {
        this.prixParMetreCarre = prixParMetreCarre;
    }

    public BigDecimal getMultiplicateurNiveau() {
        return multiplicateurNiveau;
    }

    public void setMultiplicateurNiveau(BigDecimal multiplicateurNiveau) {
        this.multiplicateurNiveau = multiplicateurNiveau;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    // Méthode pour vérifier si le prix est actif (non supprimé)
    public boolean isActive() {
        return deletedAt == null;
    }

    // Méthode de calcul du budget
    public BigDecimal calculerBudget(BigDecimal surface, Integer niveauUrgence) {
        if (surface == null || niveauUrgence == null) {
            return BigDecimal.ZERO;
        }
        
        // Budget = prix_par_m2 * niveau * surface_m2
        BigDecimal niveau = BigDecimal.valueOf(niveauUrgence);
        return prixParMetreCarre.multiply(niveau).multiply(surface);
    }

    // Méthodes de cycle de vie
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
