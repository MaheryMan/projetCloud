package com.projetCloud.app.utilisateurs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    @Autowired
    private UtilisateurService utilisateurService;

    @GetMapping
    public List<Utilisateur> getAllUsers() {
        return utilisateurService.findAll();
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
            return ResponseEntity.status(500).body("Erreur interne du serveur: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Utilisateur> updateUser(@PathVariable Long id, @RequestBody Utilisateur userDetails) {
        Optional<Utilisateur> utilisateur = utilisateurService.findById(id);
        if (utilisateur.isPresent()) {
            Utilisateur updatedUser = utilisateur.get();
            updatedUser.setEmail(userDetails.getEmail());
            updatedUser.setNom(userDetails.getNom());
            updatedUser.setPrenom(userDetails.getPrenom());
            updatedUser.setNumTel(userDetails.getNumTel());
            // etc.
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
}