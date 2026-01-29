package com.projetCloud.app.typesSignalement;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "types_signalement")
public class TypeSignalement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String libelle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String icone;

    @Column(length = 20)
    private String couleur;

    @Column(name = "niveau_urgence")
    private Integer niveauUrgence;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public TypeSignalement() {}

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public Integer getNiveauUrgence() {
        return niveauUrgence;
    }

    public void setNiveauUrgence(Integer niveauUrgence) {
        this.niveauUrgence = niveauUrgence;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
