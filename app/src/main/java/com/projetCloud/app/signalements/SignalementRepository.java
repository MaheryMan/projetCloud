package com.projetCloud.app.signalements;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignalementRepository extends JpaRepository<Signalement, Long> {

    List<Signalement> findByUtilisateurId(Long utilisateurId);
    List<Signalement> findByNiveauTravailId(Long niveauTravailId);
}