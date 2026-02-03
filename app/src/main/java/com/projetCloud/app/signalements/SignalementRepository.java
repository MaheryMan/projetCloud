package com.projetCloud.app.signalements;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignalementRepository extends JpaRepository<Signalement, Long> {

    List<Signalement> findByUtilisateurId(Long utilisateurId);
    
    /**
     * Trouve un signalement par son ID Firebase
     */
    Optional<Signalement> findByFirebaseId(String firebaseId);
    
    /**
     * Trouve tous les signalements qui doivent être synchronisés vers Firebase
     */
    List<Signalement> findByNeedsFirebaseSyncTrue();
    
    /**
     * Trouve tous les signalements avec un status spécifique
     */
    List<Signalement> findByIdStatus(Long idStatus);
}