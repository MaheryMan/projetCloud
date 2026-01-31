package com.projetCloud.app.sessions;

import com.projetCloud.app.utilisateurs.Utilisateur;
import com.projetCloud.app.utilisateurs.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UtilisateurService utilisateurService;

    @GetMapping
    public List<Session> getAllSessions() {
        return sessionService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Session> getSessionById(@PathVariable UUID id) {
        Optional<Session> session = sessionService.findById(id);
        if (session.isPresent()) {
            return ResponseEntity.ok(session.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Session> createSession(@RequestBody SessionRequest request) {
        Optional<Utilisateur> utilisateur = utilisateurService.findById(request.getIdUtilisateur());
        if (utilisateur.isPresent()) {
            Session session = new Session(request.getToken(), request.getExpiresAt(), utilisateur.get());
            session.setDeviceInfo(request.getDeviceInfo());
            session.setIpAddress(request.getIpAddress());
            if (request.getIsValid() != null) {
                session.setIsValid(request.getIsValid());
            }
            session.setLogoutAt(request.getLogoutAt());
            return ResponseEntity.ok(sessionService.save(session));
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Session> updateSession(@PathVariable UUID id, @RequestBody SessionRequest request) {
        Optional<Session> sessionOpt = sessionService.findById(id);
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            if (request.getToken() != null && !request.getToken().trim().isEmpty()) {
                session.setToken(request.getToken().trim());
            }
            if (request.getExpiresAt() != null) {
                session.setExpiresAt(request.getExpiresAt());
            }
            if (request.getIsValid() != null) {
                session.setIsValid(request.getIsValid());
            }
            if (request.getDeviceInfo() != null) {
                session.setDeviceInfo(request.getDeviceInfo());
            }
            if (request.getIpAddress() != null) {
                session.setIpAddress(request.getIpAddress());
            }
            if (request.getLogoutAt() != null) {
                session.setLogoutAt(request.getLogoutAt());
            }
            // utilisateur not updated
            return ResponseEntity.ok(sessionService.save(session));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID id) {
        if (sessionService.findById(id).isPresent()) {
            sessionService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Classe interne pour la requête
    public static class SessionRequest {
        private String token;
        private LocalDateTime expiresAt;
        private Boolean isValid;
        private String deviceInfo;
        private String ipAddress;
        private LocalDateTime logoutAt;
        private Long idUtilisateur;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }

        public Boolean getIsValid() {
            return isValid;
        }

        public void setIsValid(Boolean isValid) {
            this.isValid = isValid;
        }

        public String getDeviceInfo() {
            return deviceInfo;
        }

        public void setDeviceInfo(String deviceInfo) {
            this.deviceInfo = deviceInfo;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public LocalDateTime getLogoutAt() {
            return logoutAt;
        }

        public void setLogoutAt(LocalDateTime logoutAt) {
            this.logoutAt = logoutAt;
        }

        public Long getIdUtilisateur() {
            return idUtilisateur;
        }

        public void setIdUtilisateur(Long idUtilisateur) {
            this.idUtilisateur = idUtilisateur;
        }
    }
}