package com.projetCloud.app.status;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {

    Optional<Status> findByCode(String code);
    Optional<Status> findByLibelle(String libelle);
    List<Status> findByIsSyncedToFirebaseFalse();
}