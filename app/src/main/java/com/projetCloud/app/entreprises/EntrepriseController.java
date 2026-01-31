package com.projetCloud.app.entreprises;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/entreprises")
public class EntrepriseController {

    @Autowired
    private EntrepriseService entrepriseService;

    @GetMapping
    public List<Entreprise> getAllEntreprises() {
        return entrepriseService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Entreprise> getEntrepriseById(@PathVariable Long id) {
        Optional<Entreprise> entreprise = entrepriseService.findById(id);
        if (entreprise.isPresent()) {
            return ResponseEntity.ok(entreprise.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> createEntreprise(@RequestBody Entreprise entreprise) {
        if (entreprise.getNom() == null || entreprise.getNom().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Le nom est requis");
        }

        try {
            entreprise.setId(null);
            Entreprise saved = entrepriseService.save(entreprise);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur interne du serveur: " + e.getMessage());
        }
    }
}
