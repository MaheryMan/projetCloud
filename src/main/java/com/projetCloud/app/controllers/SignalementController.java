package com.projetCloud.app.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.*;
import com.projetCloud.app.services.SignalementService;
import com.projetCloud.app.models.Signalement;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/signalements")
public class SignalementController {
    private final SignalementService service;

    @Autowired
    public SignalementController(SignalementService service) {
        this.service = service;
    }

    @GetMapping
    public List<Signalement> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Signalement> getById(@PathVariable Integer id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Signalement> create(@RequestBody Signalement s) {
        Signalement saved = service.save(s);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Signalement> update(@PathVariable Integer id, @RequestBody Signalement s) {
        return service.findById(id).map(existing -> {
            existing.setLatitude(s.getLatitude());
            existing.setLongitude(s.getLongitude());
            existing.setSurfaceM2(s.getSurfaceM2());
            existing.setDateSignalement(s.getDateSignalement());
            existing.setDescription(s.getDescription());
            existing.setIdNiveauTravaux(s.getIdNiveauTravaux());
            existing.setIdUtilisateur(s.getIdUtilisateur());
            Signalement updated = service.save(existing);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return service.findById(id).map(existing -> {
            service.deleteById(id);
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }).orElse(ResponseEntity.notFound().build());
    }
}
