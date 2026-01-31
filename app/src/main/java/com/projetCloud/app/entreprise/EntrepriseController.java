package com.projetCloud.app.entreprise;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entreprises")
public class EntrepriseController {

    @Autowired
    private EntrepriseService entrepriseService;

    @GetMapping
    public List<Entreprise> getAll() {
        return entrepriseService.findAll();
    }

    @PostMapping
    public Entreprise create(@RequestBody Entreprise entreprise) {
        return entrepriseService.save(entreprise);
    }

    @GetMapping("/{id}")
    public Entreprise getById(@PathVariable Long id) {
        return entrepriseService.findById(id);
    }

    @PutMapping("/{id}")
    public Entreprise update(@PathVariable Long id, @RequestBody Entreprise entreprise) {
        entreprise.setId(id);
        return entrepriseService.save(entreprise);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        entrepriseService.deleteById(id);
    }
}