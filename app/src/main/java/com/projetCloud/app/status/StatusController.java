package com.projetCloud.app.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    @Autowired
    private StatusService statusService;

    @GetMapping
    public List<Status> getAllStatus() {
        return statusService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Status> getStatusById(@PathVariable Long id) {
        Optional<Status> status = statusService.findById(id);
        if (status.isPresent()) {
            return ResponseEntity.ok(status.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createStatus(@RequestBody Status status) {
        // Validation des champs requis
        if (status.getCode() == null || status.getCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Le code est requis");
        }
        if (status.getLibelle() == null || status.getLibelle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Le libelle est requis");
        }

        try {
            status.setId(null); // Ensure it's a new entity
            Status savedStatus = statusService.save(status);
            return ResponseEntity.ok(savedStatus);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Erreur de validation des données: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur interne du serveur: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Status> updateStatus(@PathVariable Long id, @RequestBody Status statusDetails) {
        Optional<Status> status = statusService.findById(id);
        if (status.isPresent()) {
            Status updatedStatus = status.get();
            updatedStatus.setCode(statusDetails.getCode());
            updatedStatus.setLibelle(statusDetails.getLibelle());
            return ResponseEntity.ok(statusService.save(updatedStatus));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStatus(@PathVariable Long id) {
        if (statusService.findById(id).isPresent()) {
            statusService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}