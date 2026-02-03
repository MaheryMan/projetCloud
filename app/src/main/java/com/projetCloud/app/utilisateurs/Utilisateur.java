package com.projetCloud.app.utilisateurs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.projetCloud.app.roles.Role;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "utilisateurs")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(name = "num_tel")
    private String numTel;

    @Column(columnDefinition = "TEXT")
    private String password;

    @Column(name = "firebase_uid", unique = true, length = 128)
    private String firebaseUid;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(name = "tentatives_connexion")
    private Integer tentativesConnexion = 0;

    @Column(name = "is_blocked")
    private Boolean isBlocked = false;

    @Column(name = "last_failed_attempt")
    private LocalDateTime lastFailedAttempt;

    @Column(name = "id_source", nullable = false)
    private Integer idSource;

    @Column(name = "id_status", nullable = false)
    private Integer idStatus;

    @Column(name = "is_synced_to_firebase")
    private Boolean isSyncedToFirebase = false;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "firebase_created_at")
    private LocalDateTime firebaseCreatedAt;

    @Column(name = "modified_offline")
    private Boolean modifiedOffline = false;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "temp_password", columnDefinition = "TEXT")
    private String tempPassword; // Mot de passe temporaire pour sync offline

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "id_utilisateur"),
        inverseJoinColumns = @JoinColumn(name = "id_role")
    )
    private Set<Role> roles;

    // Constructeurs
    public Utilisateur() {
        this.roles = new java.util.HashSet<>();
    }

    public Utilisateur(String email, String numTel, String password, String nom, String prenom, Integer idSource, Integer idStatus) {
        this.email = email;
        this.numTel = numTel;
        this.password = password;
        this.nom = nom;
        this.prenom = prenom;
        this.idSource = idSource;
        this.idStatus = idStatus;
        this.roles = new java.util.HashSet<>();
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNumTel() {
        return numTel;
    }

    public void setNumTel(String numTel) {
        this.numTel = numTel;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public Integer getTentativesConnexion() {
        return tentativesConnexion;
    }

    public void setTentativesConnexion(Integer tentativesConnexion) {
        this.tentativesConnexion = tentativesConnexion;
    }

    public Boolean getIsBlocked() {
        return isBlocked;
    }

    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }

    public LocalDateTime getLastFailedAttempt() {
        return lastFailedAttempt;
    }

    public void setLastFailedAttempt(LocalDateTime lastFailedAttempt) {
        this.lastFailedAttempt = lastFailedAttempt;
    }

    public Integer getIdSource() {
        return idSource;
    }

    public void setIdSource(Integer idSource) {
        this.idSource = idSource;
    }

    public Integer getIdStatus() {
        return idStatus;
    }

    public void setIdStatus(Integer idStatus) {
        this.idStatus = idStatus;
    }

    public Boolean getIsSyncedToFirebase() {
        return isSyncedToFirebase;
    }

    public void setIsSyncedToFirebase(Boolean isSyncedToFirebase) {
        this.isSyncedToFirebase = isSyncedToFirebase;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public LocalDateTime getFirebaseCreatedAt() {
        return firebaseCreatedAt;
    }

    public void setFirebaseCreatedAt(LocalDateTime firebaseCreatedAt) {
        this.firebaseCreatedAt = firebaseCreatedAt;
    }

    public Boolean getModifiedOffline() {
        return modifiedOffline;
    }

    public void setModifiedOffline(Boolean modifiedOffline) {
        this.modifiedOffline = modifiedOffline;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public String getTempPassword() {
        return tempPassword;
    }

    public void setTempPassword(String tempPassword) {
        this.tempPassword = tempPassword;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}