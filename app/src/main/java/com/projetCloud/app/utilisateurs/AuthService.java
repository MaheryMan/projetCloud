package com.projetCloud.app.utilisateurs;

import com.projetCloud.app.configurations.ConfigurationService;
import com.projetCloud.app.sessions.Session;
import com.projetCloud.app.sessions.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service pour gérer l'authentification et les sessions
 */
@Service
public class AuthService {

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Authentifie un utilisateur et crée une session
     */
    public Optional<AuthResponse> login(String email, String password, String deviceInfo, String ipAddress) {
        Optional<Utilisateur> userOpt = utilisateurService.authenticate(email, password);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        Utilisateur user = userOpt.get();

        // Récupérer les rôles
        List<String> roles = utilisateurService.getUserRoles(user.getId());

        // Le manager/admin a un token (opaque) sans expiration (expiration très lointaine)
        if (roles.contains("Manager")) {
            String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
            LocalDateTime expiresAt = LocalDateTime.now().plusYears(100);

            Session session = new Session();
            session.setToken(token);
            session.setUtilisateur(user);
            session.setCreatedAt(LocalDateTime.now());
            session.setExpiresAt(expiresAt);
            session.setLastActivity(LocalDateTime.now());
            session.setIsValid(true);
            session.setDeviceInfo(deviceInfo);
            session.setIpAddress(ipAddress);

            sessionRepository.save(session);

            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setUser(user);
            response.setRoles(roles);
            return Optional.of(response);
        }

        // Générer un token opaque unique
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");

        // Récupérer la durée de session depuis configurations
        int sessionDurationMinutes = Integer.parseInt(configurationService.getValue("duree_session_minutes", "1440"));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(sessionDurationMinutes);

        // Créer la session
        Session session = new Session();
        session.setToken(token);
        session.setUtilisateur(user);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(expiresAt);
        session.setLastActivity(LocalDateTime.now());
        session.setIsValid(true);
        session.setDeviceInfo(deviceInfo);
        session.setIpAddress(ipAddress);

        sessionRepository.save(session);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(user);
        response.setRoles(roles);

        return Optional.of(response);
    }

    /**
     * Valide un token et retourne l'utilisateur associé
     */
    public Optional<Utilisateur> validateToken(String token) {
        Optional<Session> sessionOpt = sessionRepository.findByToken(token);
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }

        Session session = sessionOpt.get();
        if (!session.getIsValid() || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }

        // Mettre à jour lastActivity
        session.setLastActivity(LocalDateTime.now());
        sessionRepository.save(session);

        return Optional.of(session.getUtilisateur());
    }

    /**
     * Déconnecte un utilisateur en invalidant sa session
     */
    public boolean logout(String token) {
        Optional<Session> sessionOpt = sessionRepository.findByToken(token);
        if (sessionOpt.isEmpty()) {
            return false;
        }

        Session session = sessionOpt.get();
        session.setIsValid(false);
        sessionRepository.save(session);
        return true;
    }

    /**
     * Classe de réponse pour l'authentification
     */
    public static class AuthResponse {
        private String token;
        private Utilisateur user;
        private List<String> roles;

        // Getters et setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public Utilisateur getUser() { return user; }
        public void setUser(Utilisateur user) { this.user = user; }

        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }
}