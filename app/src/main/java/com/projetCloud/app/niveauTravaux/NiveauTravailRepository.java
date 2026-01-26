package com.projetCloud.app.niveauTravaux;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NiveauTravailRepository extends JpaRepository<NiveauTravail, Long> {

    Optional<NiveauTravail> findByLibelle(String libelle);
}