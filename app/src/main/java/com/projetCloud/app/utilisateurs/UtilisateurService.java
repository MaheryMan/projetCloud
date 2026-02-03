package com.projetCloud.app.utilisateurs;

import com.projetCloud.app.configurations.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private ConfigurationService configurationService;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
                && utilisateur.get().getDeletedAt() == null) {
            
            Utilisateur user = utilisateur.get();
            
            // Vérifier si l'utilisateur est bloqué
            if (user.getIsBlocked() != null && user.getIsBlocked()) {
                // Vérifier si le délai de blocage est écoulé
                int blocageDurationMinutes = Integer.parseInt(configurationService.getValue("duree_blocage_minutes", "30"));
                if (user.getLastFailedAttempt() != null) {
                    LocalDateTime blockedUntil = user.getLastFailedAttempt().plusMinutes(blocageDurationMinutes);
                    if (LocalDateTime.now().isAfter(blockedUntil)) {
                        // Débloquer automatiquement l'utilisateur
                        user.setIsBlocked(false);
                        user.setTentativesConnexion(0);
                        utilisateurRepository.save(user);
                    } else {
                        // L'utilisateur est toujours bloqué
                        return Optional.empty();
                    }
                } else {
                    return Optional.empty();
                }
            }
            
            String storedPassword = user.getPassword();
            boolean passwordMatches = false;
            if (storedPassword != null) {
                if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2y$") || storedPassword.startsWith("$2b$")) {
                    // Password is BCrypt hashed
                    passwordMatches = passwordEncoder.matches(password, storedPassword);
                } else {
                    // Password is plain text (for backward compatibility)
                    passwordMatches = password.equals(storedPassword);
                }
            }
            if (passwordMatches) {
                // Réinitialiser le compteur de tentatives en cas de succès
                if (user.getTentativesConnexion() != null && user.getTentativesConnexion() > 0) {
                    user.setTentativesConnexion(0);
                    user.setLastFailedAttempt(null);
                    utilisateurRepository.save(user);
                }
                return Optional.of(user);
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
                .filter(u -> u.getDeletedAt() == null);
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
                .filter(u -> u.getDeletedAt() == null);
    }

    /**
     * Récupère la liste de tous les utilisateurs
     *
     * @return la liste des utilisateurs
     */
    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll().stream()
                .filter(u -> u.getDeletedAt() == null)
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
        return roles.contains("Manager");
    }

    /**
     * Enregistre un utilisateur
     *
     * @param utilisateur l'utilisateur à enregistrer
     * @return l'utilisateur enregistré
     */
    public Utilisateur save(Utilisateur utilisateur) {
        if (utilisateur.getPassword() != null) {
            String pwd = utilisateur.getPassword();
            boolean alreadyBcrypt = pwd.startsWith("$2a$") || pwd.startsWith("$2y$") || pwd.startsWith("$2b$");
            if (!alreadyBcrypt) {
                utilisateur.setPassword(passwordEncoder.encode(pwd));
            }
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
            utilisateur.setDeletedAt(LocalDateTime.now());
            utilisateurRepository.save(utilisateur);
        }
    }
}