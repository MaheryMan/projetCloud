package com.projetCloud.app.deblocages;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeblocageRepository extends JpaRepository<Deblocage, Long> {

    List<Deblocage> findByUtilisateurBloqueId(Long utilisateurId);
    List<Deblocage> findByManagerId(Long managerId);
}