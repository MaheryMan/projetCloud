package com.projetCloud.app.sources;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sources")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String libelle;

    @Column(name = "provider_type", length = 20, nullable = false)
    private String providerType;

    @Column(name = "is_online")
    private Boolean isOnline;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructeurs
    public Source() {}

    public Source(String libelle, String providerType, Boolean isOnline) {
        this.libelle = libelle;
        this.providerType = providerType;
        this.isOnline = isOnline;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}