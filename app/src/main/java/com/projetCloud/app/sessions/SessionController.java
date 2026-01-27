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
            Session session = new Session(request.getToken(), request.getExpireLe(), utilisateur.get());
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
            session.setToken(request.getToken());
            session.setExpireLe(request.getExpireLe());
            session.setEstValide(request.getEstValide());
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
        private LocalDateTime expireLe;
        private Boolean estValide;
        private Long idUtilisateur;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public LocalDateTime getExpireLe() {
            return expireLe;
        }

        public void setExpireLe(LocalDateTime expireLe) {
            this.expireLe = expireLe;
        }

        public Boolean getEstValide() {
            return estValide;
        }

        public void setEstValide(Boolean estValide) {
            this.estValide = estValide;
        }

        public Long getIdUtilisateur() {
            return idUtilisateur;
        }

        public void setIdUtilisateur(Long idUtilisateur) {
            this.idUtilisateur = idUtilisateur;
        }
    }
}