package com.projetCloud.app.deblocages;

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
        Optional<Utilisateur> utilisateurBloque = utilisateurService.findById(request.getIdUtilisateurBloque());
        Optional<Utilisateur> manager = utilisateurService.findById(request.getIdManager());

        if (utilisateurBloque.isPresent() && manager.isPresent()) {
            Deblocage deblocage = new Deblocage(request.getMotif(), utilisateurBloque.get(), manager.get());
            return ResponseEntity.ok(deblocageService.save(deblocage));
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Deblocage> updateDeblocage(@PathVariable Long id, @RequestBody DeblocageRequest request) {
        Optional<Deblocage> deblocageOpt = deblocageService.findById(id);
        if (deblocageOpt.isPresent()) {
            Deblocage deblocage = deblocageOpt.get();
            deblocage.setMotif(request.getMotif());
            // Note: dateDeblocage is set automatically, utilisateurBloque and manager not updated in this example
            return ResponseEntity.ok(deblocageService.save(deblocage));
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
        private Long idUtilisateurBloque;
        private Long idManager;

        public String getMotif() {
            return motif;
        }

        public void setMotif(String motif) {
            this.motif = motif;
        }

        public Long getIdUtilisateurBloque() {
            return idUtilisateurBloque;
        }

        public void setIdUtilisateurBloque(Long idUtilisateurBloque) {
            this.idUtilisateurBloque = idUtilisateurBloque;
        }

        public Long getIdManager() {
            return idManager;
        }

        public void setIdManager(Long idManager) {
            this.idManager = idManager;
        }
    }
}