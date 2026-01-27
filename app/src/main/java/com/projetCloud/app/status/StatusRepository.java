package com.projetCloud.app.status;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {

    Optional<Status> findByStatusId(String statusId);
    Optional<Status> findByLibelle(String libelle);
}