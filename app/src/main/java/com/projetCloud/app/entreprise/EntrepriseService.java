package com.projetCloud.app.entreprise;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EntrepriseService {
    @Autowired
    private EntrepriseRepository entrepriseRepository;

    public List<Entreprise> findAll() {
        return entrepriseRepository.findAll();
    }

    public Entreprise save(Entreprise entreprise) {
        return entrepriseRepository.save(entreprise);
    }

    public Entreprise findById(Long id) {
        return entrepriseRepository.findById(id).orElse(null);
    }

    public void deleteById(Long id) {
        entrepriseRepository.deleteById(id);
    }
}