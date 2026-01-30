package com.projetCloud.app.typesSignalement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TypeSignalementRepository extends JpaRepository<TypeSignalement, Long> {

    Optional<TypeSignalement> findByLibelle(String libelle);
}
