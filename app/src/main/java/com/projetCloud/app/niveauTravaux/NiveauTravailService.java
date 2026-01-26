package com.projetCloud.app.niveauTravaux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NiveauTravailService {

    @Autowired
    private NiveauTravailRepository niveauTravailRepository;

    public List<NiveauTravail> findAll() {
        return niveauTravailRepository.findAll();
    }

    public Optional<NiveauTravail> findById(Long id) {
        return niveauTravailRepository.findById(id);
    }

    public NiveauTravail save(NiveauTravail niveauTravail) {
        return niveauTravailRepository.save(niveauTravail);
    }

    public void deleteById(Long id) {
        niveauTravailRepository.deleteById(id);
    }

    public Optional<NiveauTravail> findByLibelle(String libelle) {
        return niveauTravailRepository.findByLibelle(libelle);
    }
}