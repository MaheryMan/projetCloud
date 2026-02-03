package com.projetCloud.app.entreprises;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {
    List<Entreprise> findByIsSyncedToFirebaseFalse();
}
