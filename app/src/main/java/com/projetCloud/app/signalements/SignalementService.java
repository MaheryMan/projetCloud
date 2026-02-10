package com.projetCloud.app.signalements;

import com.projetCloud.app.historiques.HistoriqueStatusSignalementService;
import com.projetCloud.app.prixForfaitaire.PrixForfaitaireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SignalementService {

    @Autowired
    private SignalementRepository signalementRepository;

    @Autowired
    private HistoriqueStatusSignalementService historiqueService;

    @Autowired
    private PrixForfaitaireService prixForfaitaireService;

    public List<Signalement> findAll() {
        List<Signalement> signalements = signalementRepository.findAll();
        // Enrichir chaque signalement avec la date de dernière mise à jour depuis historique
        signalements.forEach(this::enrichWithHistoriqueDate);
        return signalements;
    }

    public Optional<Signalement> findById(Long id) {
        Optional<Signalement> signalement = signalementRepository.findById(id);
        signalement.ifPresent(this::enrichWithHistoriqueDate);
        return signalement;
    }

    public Signalement save(Signalement signalement) {
        // Calculer automatiquement le budget si surface et niveau sont présents
        if (signalement.getSurfaceM2() != null && signalement.getNiveau() != null) {
            try {
                BigDecimal budgetCalcule = prixForfaitaireService.calculerBudget(
                    signalement.getSurfaceM2(), 
                    signalement.getNiveau()
                );
                signalement.setBudget(budgetCalcule);
            } catch (IllegalStateException e) {
                // Si aucun prix forfaitaire n'est configuré, on garde le budget actuel
                System.out.println("Attention: " + e.getMessage());
            }
        }
        
        return signalementRepository.save(signalement);
    }

    public void deleteById(Long id) {
        signalementRepository.deleteById(id);
    }

    public List<Signalement> findByUtilisateurId(Long utilisateurId) {
        List<Signalement> signalements = signalementRepository.findByUtilisateurId(utilisateurId);
        signalements.forEach(this::enrichWithHistoriqueDate);
        return signalements;
    }

    /**
     * Enrichit un signalement avec la date de dernière mise à jour depuis l'historique
     */
    private void enrichWithHistoriqueDate(Signalement signalement) {
        if (signalement.getId() != null) {
            Optional<LocalDateTime> lastDate = historiqueService.getLatestUpdateDate(signalement.getId());
            lastDate.ifPresent(signalement::setLastHistoriqueDate);
        }
    }
}