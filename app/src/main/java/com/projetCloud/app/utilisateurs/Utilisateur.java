package com.projetCloud.app.utilisateurs;

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

    @Column(name = "num_tel", unique = true)
    private String numTel;

    private String password;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer tentatives = 0;

    @Column(name = "cree_le")
    private LocalDateTime creeLe;

    @Column(name = "update_le")
    private LocalDateTime updateLe;

    @Column(name = "delete_le")
    private LocalDateTime deleteLe;

    @Column(name = "id_source", nullable = false)
    private Integer idSource;

    @Column(name = "id_status", nullable = false)
    private Integer idStatus;

    @ManyToMany
    @JoinTable(
        name = "users_roles",
        joinColumns = @JoinColumn(name = "id_utilisateur"),
        inverseJoinColumns = @JoinColumn(name = "id_role")
    )
    private Set<Role> roles;

    // Constructeurs
    public Utilisateur() {}

    public Utilisateur(String email, String numTel, String password, String nom, String prenom, Integer idSource, Integer idStatus) {
        this.email = email;
        this.numTel = numTel;
        this.password = password;
        this.nom = nom;
        this.prenom = prenom;
        this.idSource = idSource;
        this.idStatus = idStatus;
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

    public Integer getTentatives() {
        return tentatives;
    }

    public void setTentatives(Integer tentatives) {
        this.tentatives = tentatives;
    }

    public LocalDateTime getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(LocalDateTime creeLe) {
        this.creeLe = creeLe;
    }

    public LocalDateTime getUpdateLe() {
        return updateLe;
    }

    public void setUpdateLe(LocalDateTime updateLe) {
        this.updateLe = updateLe;
    }

    public LocalDateTime getDeleteLe() {
        return deleteLe;
    }

    public void setDeleteLe(LocalDateTime deleteLe) {
        this.deleteLe = deleteLe;
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

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}