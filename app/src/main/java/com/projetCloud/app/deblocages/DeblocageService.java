package com.projetCloud.app.deblocages;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DeblocageService {

    @Autowired
    private DeblocageRepository deblocageRepository;

    public List<Deblocage> findAll() {
        return deblocageRepository.findAll();
    }

    public Optional<Deblocage> findById(Long id) {
        return deblocageRepository.findById(id);
    }

    public Deblocage save(Deblocage deblocage) {
        return deblocageRepository.save(deblocage);
    }

    public void deleteById(Long id) {
        deblocageRepository.deleteById(id);
    }

    public List<Deblocage> findByUtilisateurBloqueId(Long utilisateurId) {
        return deblocageRepository.findByUtilisateurBloqueId(utilisateurId);
    }

    public List<Deblocage> findByManagerId(Long managerId) {
        return deblocageRepository.findByManagerId(managerId);
    }
}