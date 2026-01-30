package com.projetCloud.app.niveauTravaux;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
public interface NiveauTravailRepository extends JpaRepository<NiveauTravail, Long> {

    Optional<NiveauTravail> findByLibelle(String libelle);
}