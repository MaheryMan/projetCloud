package com.projetCloud.app.prixForfaitaire;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PrixForfaitaireService {

    @Autowired
    private PrixForfaitaireRepository prixForfaitaireRepository;

    /**
     * Récupère tous les prix forfaitaires
     */
    public List<PrixForfaitaire> findAll() {
        return prixForfaitaireRepository.findAll();
    }

    /**
     * Récupère un prix forfaitaire par son ID
     */
    public Optional<PrixForfaitaire> findById(Long id) {
        return prixForfaitaireRepository.findById(id);
    }

    /**
     * Récupère le prix forfaitaire actif
     */
    public Optional<PrixForfaitaire> findActivePrix() {
        return prixForfaitaireRepository.findActivePrix();
    }

    /**
     * Crée un nouveau prix forfaitaire et désactive les anciens
     */
    public PrixForfaitaire createNewPrix(BigDecimal prixParMetreCarre, BigDecimal multiplicateurNiveau) {
        // Désactiver tous les prix existants
        List<PrixForfaitaire> anciensPrix = prixForfaitaireRepository.findAll();
        for (PrixForfaitaire ancienPrix : anciensPrix) {
            if (ancienPrix.isActive()) {
                ancienPrix.setDeletedAt(LocalDateTime.now());
                prixForfaitaireRepository.save(ancienPrix);
            }
        }

        // Créer le nouveau prix
        PrixForfaitaire nouveauPrix = new PrixForfaitaire(prixParMetreCarre, multiplicateurNiveau);
        return prixForfaitaireRepository.save(nouveauPrix);
    }

    /**
     * Met à jour le prix forfaitaire actif
     */
    public PrixForfaitaire updatePrix(BigDecimal prixParMetreCarre, BigDecimal multiplicateurNiveau) {
        Optional<PrixForfaitaire> activePrixOpt = findActivePrix();
        
        if (activePrixOpt.isPresent()) {
            // Désactiver l'ancien prix
            PrixForfaitaire ancienPrix = activePrixOpt.get();
            ancienPrix.setDeletedAt(LocalDateTime.now());
            prixForfaitaireRepository.save(ancienPrix);
        }

        // Créer un nouveau prix
        return createNewPrix(prixParMetreCarre, multiplicateurNiveau);
    }

    /**
     * Calcule le budget pour un signalement
     */
    public BigDecimal calculerBudget(BigDecimal surface, Integer niveauUrgence) {
        Optional<PrixForfaitaire> prixOpt = findActivePrix();
        
        if (prixOpt.isEmpty()) {
            throw new IllegalStateException("Aucun prix forfaitaire actif trouvé. Veuillez configurer un prix.");
        }

        PrixForfaitaire prix = prixOpt.get();
        return prix.calculerBudget(surface, niveauUrgence);
    }

    /**
     * Supprime définitivement un prix forfaitaire
     */
    public void deleteById(Long id) {
        prixForfaitaireRepository.deleteById(id);
    }

    /**
     * Désactive (soft delete) un prix forfaitaire
     */
    public void deactivatePrix(Long id) {
        Optional<PrixForfaitaire> prixOpt = findById(id);
        if (prixOpt.isPresent()) {
            PrixForfaitaire prix = prixOpt.get();
            prix.setDeletedAt(LocalDateTime.now());
            prixForfaitaireRepository.save(prix);
        }
    }
}
