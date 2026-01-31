package com.projetCloud.app.typesSignalement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/types-signalement")
public class TypeSignalementController {

    @Autowired
    private TypeSignalementService typeSignalementService;

    @GetMapping
    public List<TypeSignalement> getAllTypesSignalement() {
        return typeSignalementService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TypeSignalement> getTypeSignalementById(@PathVariable Long id) {
        Optional<TypeSignalement> typeSignalement = typeSignalementService.findById(id);
        if (typeSignalement.isPresent()) {
            return ResponseEntity.ok(typeSignalement.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createTypeSignalement(@RequestBody TypeSignalement typeSignalement) {
        if (typeSignalement.getLibelle() == null || typeSignalement.getLibelle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Le libelle est requis");
        }

        try {
            typeSignalement.setId(null);
            TypeSignalement saved = typeSignalementService.save(typeSignalement);
            return ResponseEntity.ok(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("libelle")) {
                return ResponseEntity.badRequest().body("Libelle déjà utilisé");
            }
            return ResponseEntity.badRequest().body("Erreur de validation des données: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur interne du serveur: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TypeSignalement> updateTypeSignalement(@PathVariable Long id, @RequestBody TypeSignalement typeSignalementDetails) {
        Optional<TypeSignalement> typeSignalement = typeSignalementService.findById(id);
        if (typeSignalement.isPresent()) {
            TypeSignalement updated = typeSignalement.get();
            if (typeSignalementDetails.getLibelle() != null && !typeSignalementDetails.getLibelle().trim().isEmpty()) {
                updated.setLibelle(typeSignalementDetails.getLibelle().trim());
            }
            if (typeSignalementDetails.getDescription() != null) {
                updated.setDescription(typeSignalementDetails.getDescription());
            }
            if (typeSignalementDetails.getIcone() != null) {
                updated.setIcone(typeSignalementDetails.getIcone());
            }
            if (typeSignalementDetails.getCouleur() != null) {
                updated.setCouleur(typeSignalementDetails.getCouleur());
            }
            if (typeSignalementDetails.getNiveauUrgence() != null) {
                updated.setNiveauUrgence(typeSignalementDetails.getNiveauUrgence());
            }
            return ResponseEntity.ok(typeSignalementService.save(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTypeSignalement(@PathVariable Long id) {
        if (typeSignalementService.findById(id).isPresent()) {
            typeSignalementService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
