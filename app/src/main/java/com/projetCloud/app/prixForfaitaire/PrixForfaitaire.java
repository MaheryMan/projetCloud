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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Constructeurs
    public PrixForfaitaire() {}

    public PrixForfaitaire(BigDecimal prixParMetreCarre) {
        this.prixParMetreCarre = prixParMetreCarre;
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
    public BigDecimal calculerBudget(BigDecimal surface, Integer niveau) {
        if (surface == null || niveau == null) {
            return BigDecimal.ZERO;
        }
        
        // Budget = prix_par_m2 * niveau * surface_m2
        BigDecimal niveauDecimal = BigDecimal.valueOf(niveau);
        return prixParMetreCarre.multiply(niveauDecimal).multiply(surface);
    }

    // Méthodes de cycle de vie
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
