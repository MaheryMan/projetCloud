package com.projetCloud.app.niveauTravaux;

import jakarta.persistence.*;

@Entity
@Table(name = "niveau_travaux")
public class NiveauTravail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String libelle;

    @Column(nullable = false)
    private Integer niveau;

    // Constructeurs
    public NiveauTravail() {}

    public NiveauTravail(String libelle, Integer niveau) {
        this.libelle = libelle;
        this.niveau = niveau;
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

    public Integer getNiveau() {
        return niveau;
    }

    public void setNiveau(Integer niveau) {
        this.niveau = niveau;
    }
}