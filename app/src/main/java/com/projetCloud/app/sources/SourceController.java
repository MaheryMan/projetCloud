package com.projetCloud.app.sources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/sources")
public class SourceController {

    @Autowired
    private SourceService sourceService;

    @GetMapping
    public List<Source> getAllSources() {
        return sourceService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Source> getSourceById(@PathVariable Long id) {
        Optional<Source> source = sourceService.findById(id);
        if (source.isPresent()) {
            return ResponseEntity.ok(source.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createSource(@RequestBody Source source) {
        if (source.getProviderType() == null || source.getProviderType().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Le providerType est requis");
        }
        try {
            source.setId(null); // Ensure it's a new entity
            Source savedSource = sourceService.save(source);
            return ResponseEntity.ok(savedSource);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur interne du serveur: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Source> updateSource(@PathVariable Long id, @RequestBody Source sourceDetails) {
        Optional<Source> source = sourceService.findById(id);
        if (source.isPresent()) {
            Source updated = source.get();
            if (sourceDetails.getLibelle() != null && !sourceDetails.getLibelle().trim().isEmpty()) {
                updated.setLibelle(sourceDetails.getLibelle().trim());
            }
            if (sourceDetails.getProviderType() != null && !sourceDetails.getProviderType().trim().isEmpty()) {
                updated.setProviderType(sourceDetails.getProviderType().trim());
            }
            if (sourceDetails.getIsOnline() != null) {
                updated.setIsOnline(sourceDetails.getIsOnline());
            }
            return ResponseEntity.ok(sourceService.save(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable Long id) {
        if (sourceService.findById(id).isPresent()) {
            sourceService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}