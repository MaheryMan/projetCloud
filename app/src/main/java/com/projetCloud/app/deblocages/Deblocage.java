package com.projetCloud.app.deblocages;

import com.projetCloud.app.utilisateurs.Utilisateur;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deblocages")
public class Deblocage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_deblocage")
    private LocalDateTime dateDeblocage;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @ManyToOne
    @JoinColumn(name = "id_utilisateur_bloque", nullable = false)
    private Utilisateur utilisateurBloque;

    @ManyToOne
    @JoinColumn(name = "id_manager", nullable = false)
    private Utilisateur manager;

    // Constructeurs
    public Deblocage() {}

    public Deblocage(String motif, Utilisateur utilisateurBloque, Utilisateur manager) {
        this.motif = motif;
        this.utilisateurBloque = utilisateurBloque;
        this.manager = manager;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDateDeblocage() {
        return dateDeblocage;
    }

    public void setDateDeblocage(LocalDateTime dateDeblocage) {
        this.dateDeblocage = dateDeblocage;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Utilisateur getUtilisateurBloque() {
        return utilisateurBloque;
    }

    public void setUtilisateurBloque(Utilisateur utilisateurBloque) {
        this.utilisateurBloque = utilisateurBloque;
    }

    public Utilisateur getManager() {
        return manager;
    }

    public void setManager(Utilisateur manager) {
        this.manager = manager;
    }
}