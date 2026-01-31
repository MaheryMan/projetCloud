package com.projetCloud.app.deblocages;

import com.projetCloud.app.configurations.ConfigurationService;
import com.projetCloud.app.utilisateurs.Utilisateur;
import com.projetCloud.app.utilisateurs.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/deblocages")
public class DeblocageController {

    @Autowired
    private DeblocageService deblocageService;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private ConfigurationService configurationService;

    @GetMapping
    public List<Deblocage> getAllDeblocages() {
        return deblocageService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Deblocage> getDeblocageById(@PathVariable Long id) {
        Optional<Deblocage> deblocage = deblocageService.findById(id);
        if (deblocage.isPresent()) {
            return ResponseEntity.ok(deblocage.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Deblocage> createDeblocage(@RequestBody DeblocageRequest request) {
        Optional<Utilisateur> utilisateur = utilisateurService.findById(request.getIdUtilisateur());
        
        Utilisateur manager;
        if (request.getIdManager() != null) {
            Optional<Utilisateur> managerOpt = utilisateurService.findById(request.getIdManager());
            if (managerOpt.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            manager = managerOpt.get();
        } else {
            // Utiliser le manager par défaut
            String defaultManagerEmail = configurationService.getDefaultManagerEmail();
            Optional<Utilisateur> defaultManagerOpt = utilisateurService.findByEmail(defaultManagerEmail);
            if (defaultManagerOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(null); // Manager par défaut non trouvé
            }
            manager = defaultManagerOpt.get();
        }

        if (utilisateur.isPresent()) {
            Deblocage deblocage = new Deblocage(request.getMotif(), utilisateur.get(), manager);
            return ResponseEntity.ok(deblocageService.save(deblocage));
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Deblocage> updateDeblocage(@PathVariable Long id, @RequestBody Deblocage deblocageDetails) {
        Optional<Deblocage> deblocage = deblocageService.findById(id);
        if (deblocage.isPresent()) {
            Deblocage updatedDeblocage = deblocage.get();
            if (deblocageDetails.getMotif() != null) {
                updatedDeblocage.setMotif(deblocageDetails.getMotif());
            }
            return ResponseEntity.ok(deblocageService.save(updatedDeblocage));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeblocage(@PathVariable Long id) {
        if (deblocageService.findById(id).isPresent()) {
            deblocageService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Classe interne pour la requête
    public static class DeblocageRequest {
        private String motif;
        private Long idUtilisateur;
        private Long idManager;

        public String getMotif() {
            return motif;
        }

        public void setMotif(String motif) {
            this.motif = motif;
        }

        public Long getIdUtilisateur() {
            return idUtilisateur;
        }

        public void setIdUtilisateur(Long idUtilisateur) {
            this.idUtilisateur = idUtilisateur;
        }

        public Long getIdManager() {
            return idManager;
        }

        public void setIdManager(Long idManager) {
            this.idManager = idManager;
        }
    }
}