package com.projetCloud.app.utilisateurs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.projetCloud.app.deblocages.Deblocage;
import com.projetCloud.app.deblocages.DeblocageService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private DeblocageService deblocageService;

    @GetMapping
    public List<Utilisateur> getAllUsers() {
        return utilisateurService.findAll();
    }

    @GetMapping("/blocked")
    public List<Utilisateur> getBlockedUsers() {
        return utilisateurService.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsBlocked()))
                .toList();
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<?> unblockUser(@PathVariable Long id) {
        Optional<Utilisateur> user = utilisateurService.findById(id);
        if (user.isPresent()) {
            Utilisateur u = user.get();
            u.setIsBlocked(false);
            utilisateurService.save(u);
            Utilisateur manager = new Utilisateur();
            manager.setId(1L);
            Deblocage deblocage = new Deblocage("debloque par manager", u, manager);
            deblocageService.save(deblocage);

            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint sécurisé pour débloquer un compte
     * Vérifie que l'utilisateur effectuant la requête a le rôle Manager
     * 
     * @param userId L'ID de l'utilisateur à débloquer
     * @param managerId L'ID du manager (celui qui effectue le déblocage)
     * @param request Contient optionnellement le motif du déblocage
     * @return L'utilisateur débloqué ou une erreur
     */
    @PostMapping("/admin/unlock-account/{userId}")
    public ResponseEntity<?> adminUnlockAccount(
            @PathVariable Long userId,
            @RequestParam Long managerId,
            @RequestBody(required = false) UnlockRequest request) {
        
        try {
            // Vérifier que le manager existe et a le rôle Manager
            Optional<Utilisateur> managerOpt = utilisateurService.findById(managerId);
            if (managerOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Manager non trouvé"));
            }

            Utilisateur manager = managerOpt.get();
            
            // Vérifier que le manager a bien le rôle Manager
            boolean isManager = manager.getRoles().stream()
                    .anyMatch(role -> "Manager".equals(role.getLibelle()));
            
            if (!isManager) {
                return ResponseEntity.status(403)
                        .body(new ErrorResponse("Seul un utilisateur avec le rôle Manager peut débloquer un compte"));
            }

            // Récupérer le motif du déblocage
            String motif = (request != null && request.getMotif() != null) 
                    ? request.getMotif() 
                    : "Déblocage manuel par manager";

            // Débloquer l'utilisateur
            Optional<Utilisateur> unlockedUser = deblocageService.unlockUser(userId, managerId, motif);
            
            if (unlockedUser.isPresent()) {
                return ResponseEntity.ok(new UnlockResponse(
                        true,
                        "Compte débloqué avec succès",
                        unlockedUser.get()
                ));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Erreur lors du déblocage: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Utilisateur> getUserById(@PathVariable Long id) {
        Optional<Utilisateur> utilisateur = utilisateurService.findById(id);
        if (utilisateur.isPresent()) {
            return ResponseEntity.ok(utilisateur.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Utilisateur utilisateur) {
        // Validation des champs requis
        if (utilisateur.getEmail() == null || utilisateur.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("L'email est requis");
        }
        if (utilisateur.getNom() == null || utilisateur.getNom().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Le nom est requis");
        }
        if (utilisateur.getPrenom() == null || utilisateur.getPrenom().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Le prénom est requis");
        }
        if (utilisateur.getIdSource() == null) {
            return ResponseEntity.badRequest().body("La source est requise");
        }
        if (utilisateur.getIdStatus() == null) {
            return ResponseEntity.badRequest().body("Le statut est requis");
        }

        try {
            utilisateur.setId(null); // Ensure it's a new entity
            Utilisateur savedUser = utilisateurService.save(utilisateur);
            return ResponseEntity.ok(savedUser);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (e.getMessage().contains("email")) {
                return ResponseEntity.badRequest().body("Email déjà utilisé");
            } else if (e.getMessage().contains("num_tel")) {
                return ResponseEntity.badRequest().body("Numéro de téléphone déjà utilisé");
            } else if (e.getMessage().contains("fk_source") || e.getMessage().contains("fk_status")) {
                return ResponseEntity.badRequest().body("Source ou statut invalide");
            } else {
                return ResponseEntity.badRequest().body("Erreur de validation des données: " + e.getMessage());
            }
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }

            String msg = root.getMessage() != null ? root.getMessage() : e.getMessage();
            if (msg != null && (msg.contains("Un utilisateur Firebase/Google ne doit pas avoir de mot de passe local")
                    || msg.contains("Un utilisateur local doit avoir un mot de passe"))) {
                return ResponseEntity.badRequest().body(msg);
            }

            return ResponseEntity.status(500).body("Erreur interne du serveur: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Utilisateur> updateUser(@PathVariable Long id, @RequestBody Utilisateur userDetails) {
        Optional<Utilisateur> utilisateur = utilisateurService.findById(id);
        if (utilisateur.isPresent()) {
            Utilisateur updatedUser = utilisateur.get();
            if (userDetails.getEmail() != null && !userDetails.getEmail().trim().isEmpty()) {
                updatedUser.setEmail(userDetails.getEmail().trim());
            }
            if (userDetails.getNom() != null && !userDetails.getNom().trim().isEmpty()) {
                updatedUser.setNom(userDetails.getNom().trim());
            }
            if (userDetails.getPrenom() != null && !userDetails.getPrenom().trim().isEmpty()) {
                updatedUser.setPrenom(userDetails.getPrenom().trim());
            }
            if (userDetails.getNumTel() != null) {
                updatedUser.setNumTel(userDetails.getNumTel());
            }
            return ResponseEntity.ok(utilisateurService.save(updatedUser));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (utilisateurService.findById(id).isPresent()) {
            utilisateurService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Classes internes pour les réponses
    public static class UnlockRequest {
        private String motif;

        public UnlockRequest() {}

        public UnlockRequest(String motif) {
            this.motif = motif;
        }

        public String getMotif() {
            return motif;
        }

        public void setMotif(String motif) {
            this.motif = motif;
        }
    }

    public static class UnlockResponse {
        private boolean success;
        private String message;
        private Utilisateur data;

        public UnlockResponse(boolean success, String message, Utilisateur data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Utilisateur getData() {
            return data;
        }

        public void setData(Utilisateur data) {
            this.data = data;
        }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}