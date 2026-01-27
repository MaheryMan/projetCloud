package com.projetCloud.app.utilisateurs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service pour gérer les utilisateurs
 */
@Service
public class UtilisateurService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Authentifie un utilisateur
     *
     * @param email    l'email de l'utilisateur
     * @param password le mot de passe de l'utilisateur
     * @return l'utilisateur authentifié si les informations sont correctes, sinon un Optional vide
     */
    public Optional<Utilisateur> authenticate(String email, String password) {
        Optional<Utilisateur> utilisateur = utilisateurRepository.findByEmail(email);
        if (utilisateur.isPresent()
                && utilisateur.get().getDeleteLe() == null) {
            String storedPassword = utilisateur.get().getPassword();
            boolean passwordMatches = false;
            if (storedPassword != null) {
                if (storedPassword.startsWith("$2a$")) {
                    // Password is BCrypt hashed
                    passwordMatches = passwordEncoder.matches(password, storedPassword);
                } else {
                    // Password is plain text (for backward compatibility)
                    passwordMatches = password.equals(storedPassword);
                }
            }
            if (passwordMatches) {
                return utilisateur;
            }
        }
        return Optional.empty();
    }

    /**
     * Recherche un utilisateur par son email
     *
     * @param email l'email de l'utilisateur
     * @return l'utilisateur si trouvé, sinon un Optional vide
     */
    public Optional<Utilisateur> findByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .filter(u -> u.getDeleteLe() == null);
    }

    /**
     * Recherche un utilisateur par son id
     *
     * @param id l'id de l'utilisateur
     * @return l'utilisateur si trouvé, sinon un Optional vide
     */
    public Optional<Utilisateur> findByUid(Long id) {
        return utilisateurRepository.findById(id);
    }

    /**
     * Recherche un utilisateur par son id
     *
     * @param id l'id de l'utilisateur
     * @return l'utilisateur si trouvé, sinon un Optional vide
     */
    public Optional<Utilisateur> findById(Long id) {
        return utilisateurRepository.findById(id)
                .filter(u -> u.getDeleteLe() == null);
    }

    /**
     * Récupère la liste de tous les utilisateurs
     *
     * @return la liste des utilisateurs
     */
    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll().stream()
                .filter(u -> u.getDeleteLe() == null)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les rôles d'un utilisateur
     *
     * @param utilisateurId l'id de l'utilisateur
     * @return la liste des rôles de l'utilisateur
     */
    public List<String> getUserRoles(Long utilisateurId) {
        return utilisateurRepository.findRoleLibellesByUtilisateurId(utilisateurId);
    }

    /**
     * Vérifie si un utilisateur est manager
     *
     * @param utilisateurId l'id de l'utilisateur
     * @return true si l'utilisateur est manager, false sinon
     */
    public boolean isManager(Long utilisateurId) {
        List<String> roles = getUserRoles(utilisateurId);
        return roles.contains("manager");
    }

    /**
     * Enregistre un utilisateur
     *
     * @param utilisateur l'utilisateur à enregistrer
     * @return l'utilisateur enregistré
     */
    public Utilisateur save(Utilisateur utilisateur) {
        if (utilisateur.getPassword() != null && !utilisateur.getPassword().startsWith("$2a$")) {
            // Encode plain text passwords
            utilisateur.setPassword(passwordEncoder.encode(utilisateur.getPassword()));
        }
        return utilisateurRepository.save(utilisateur);
    }

    /**
     * Supprime un utilisateur
     *
     * @param id l'id de l'utilisateur à supprimer
     */
    public void deleteById(Long id) {
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(id);
        if (utilisateurOpt.isPresent()) {
            Utilisateur utilisateur = utilisateurOpt.get();
            utilisateur.setDeleteLe(LocalDateTime.now());
            utilisateurRepository.save(utilisateur);
        }
    }
}