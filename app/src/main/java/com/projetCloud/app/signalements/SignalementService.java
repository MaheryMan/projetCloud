package com.projetCloud.app.signalements;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SignalementService {

    @Autowired
    private SignalementRepository signalementRepository;

    public List<Signalement> findAll() {
        return signalementRepository.findAll();
    }

    public Optional<Signalement> findById(Long id) {
        return signalementRepository.findById(id);
    }

    public Signalement save(Signalement signalement) {
        return signalementRepository.save(signalement);
    }

    public void deleteById(Long id) {
        signalementRepository.deleteById(id);
    }

    public List<Signalement> findByUtilisateurId(Long utilisateurId) {
        return signalementRepository.findByUtilisateurId(utilisateurId);
    }

    public List<Signalement> findByNiveauTravailId(Long niveauTravailId) {
        return signalementRepository.findByNiveauTravailId(niveauTravailId);
    }
}