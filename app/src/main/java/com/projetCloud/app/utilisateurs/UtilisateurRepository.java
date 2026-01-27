package com.projetCloud.app.utilisateurs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findById(Long id);
    Optional<Utilisateur> findByEmail(String email);

    @Query("SELECT r.libelle FROM Utilisateur u JOIN u.roles r WHERE u.id = ?1")
    List<String> findRoleLibellesByUtilisateurId(Long utilisateurId);
}