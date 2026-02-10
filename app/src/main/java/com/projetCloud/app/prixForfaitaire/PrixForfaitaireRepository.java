package com.projetCloud.app.prixForfaitaire;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrixForfaitaireRepository extends JpaRepository<PrixForfaitaire, Long> {

    /**
     * Récupère le prix forfaitaire actif (non supprimé)
     * Il ne devrait y avoir qu'un seul prix actif à la fois
     */
    @Query("SELECT p FROM PrixForfaitaire p WHERE p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    Optional<PrixForfaitaire> findActivePrix();

    /**
     * Récupère le dernier prix forfaitaire actif
     */
    Optional<PrixForfaitaire> findFirstByDeletedAtIsNullOrderByCreatedAtDesc();
}
