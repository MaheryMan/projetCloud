package com.projetCloud.app.historiques;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoriqueStatusSignalementRepository extends JpaRepository<HistoriqueStatusSignalement, Long> {

    /**
     * Récupère tous les historiques d'un signalement
     */
    List<HistoriqueStatusSignalement> findByIdSignalementOrderByCreatedAtDesc(Long idSignalement);

    /**
     * Récupère le dernier historique d'un signalement (la date la plus récente)
     */
    @Query("SELECT h FROM HistoriqueStatusSignalement h WHERE h.idSignalement = :idSignalement ORDER BY h.createdAt DESC")
    Optional<HistoriqueStatusSignalement> findLatestByIdSignalement(@Param("idSignalement") Long idSignalement);

    /**
     * Récupère la date de dernière mise à jour d'un signalement
     */
    @Query("SELECT MAX(h.createdAt) FROM HistoriqueStatusSignalement h WHERE h.idSignalement = :idSignalement")
    Optional<LocalDateTime> findLatestCreatedAtByIdSignalement(@Param("idSignalement") Long idSignalement);
}
