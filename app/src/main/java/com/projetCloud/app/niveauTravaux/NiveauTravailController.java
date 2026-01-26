package com.projetCloud.app.niveauTravaux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/niveau-travaux")
public class NiveauTravailController {

    @Autowired
    private NiveauTravailService niveauTravailService;

    @GetMapping
    public List<NiveauTravail> getAllNiveauTravaux() {
        return niveauTravailService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<NiveauTravail> getNiveauTravailById(@PathVariable Long id) {
        Optional<NiveauTravail> niveauTravail = niveauTravailService.findById(id);
        if (niveauTravail.isPresent()) {
            return ResponseEntity.ok(niveauTravail.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createNiveauTravail(@RequestBody NiveauTravail niveauTravail) {
        // Validation des champs requis
        if (niveauTravail.getLibelle() == null || niveauTravail.getLibelle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Le libelle est requis");
        }
        if (niveauTravail.getNiveau() == null) {
            return ResponseEntity.badRequest().body("Le niveau est requis");
        }

        try {
            niveauTravail.setId(null); // Ensure it's a new entity
            NiveauTravail savedNiveauTravail = niveauTravailService.save(niveauTravail);
            return ResponseEntity.ok(savedNiveauTravail);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur interne du serveur: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<NiveauTravail> updateNiveauTravail(@PathVariable Long id, @RequestBody NiveauTravail niveauTravailDetails) {
        Optional<NiveauTravail> niveauTravail = niveauTravailService.findById(id);
        if (niveauTravail.isPresent()) {
            NiveauTravail updatedNiveauTravail = niveauTravail.get();
            updatedNiveauTravail.setLibelle(niveauTravailDetails.getLibelle());
            updatedNiveauTravail.setNiveau(niveauTravailDetails.getNiveau());
            return ResponseEntity.ok(niveauTravailService.save(updatedNiveauTravail));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNiveauTravail(@PathVariable Long id) {
        if (niveauTravailService.findById(id).isPresent()) {
            niveauTravailService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}