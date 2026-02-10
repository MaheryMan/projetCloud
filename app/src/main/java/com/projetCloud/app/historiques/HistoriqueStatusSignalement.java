package com.projetCloud.app.historiques;

import com.projetCloud.app.signalements.Signalement;
import com.projetCloud.app.status.Status;
import com.projetCloud.app.utilisateurs.Utilisateur;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historiques_status_signalement")
public class HistoriqueStatusSignalement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_signalement", nullable = false)
    private Long idSignalement;

    @Column(name = "id_status", nullable = false)
    private Long idStatus;

    @Column(name = "id_utilisateur", nullable = false)
    private Long idUtilisateur;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructeurs
    public HistoriqueStatusSignalement() {}

    public HistoriqueStatusSignalement(Long idSignalement, Long idStatus, Long idUtilisateur, String commentaire) {
        this.idSignalement = idSignalement;
        this.idStatus = idStatus;
        this.idUtilisateur = idUtilisateur;
        this.commentaire = commentaire;
        this.createdAt = LocalDateTime.now();
    }

    public HistoriqueStatusSignalement(Long idSignalement, Long idStatus, Long idUtilisateur, String commentaire, LocalDateTime dateModification) {
        this.idSignalement = idSignalement;
        this.idStatus = idStatus;
        this.idUtilisateur = idUtilisateur;
        this.commentaire = commentaire;
        this.createdAt = dateModification != null ? dateModification : LocalDateTime.now();
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdSignalement() {
        return idSignalement;
    }

    public void setIdSignalement(Long idSignalement) {
        this.idSignalement = idSignalement;
    }

    public Long getIdStatus() {
        return idStatus;
    }

    public void setIdStatus(Long idStatus) {
        this.idStatus = idStatus;
    }

    public Long getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(Long idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
