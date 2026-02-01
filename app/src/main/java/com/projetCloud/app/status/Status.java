package com.projetCloud.app.status;

import jakarta.persistence.*;

@Entity
@Table(name = "status")
public class Status {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String libelle;

    @Column(name = "is_synced_to_firebase")
    private Boolean isSyncedToFirebase = false;

    @Column(name = "last_synced_at")
    private java.time.LocalDateTime lastSyncedAt;

    // Constructeurs
    public Status() {}

    public Status(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Boolean getIsSyncedToFirebase() {
        return isSyncedToFirebase;
    }

    public void setIsSyncedToFirebase(Boolean isSyncedToFirebase) {
        this.isSyncedToFirebase = isSyncedToFirebase;
    }

    public java.time.LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(java.time.LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}