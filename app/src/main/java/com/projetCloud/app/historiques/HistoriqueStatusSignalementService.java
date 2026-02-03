package com.projetCloud.app.historiques;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class HistoriqueStatusSignalementService {

    @Autowired
    private HistoriqueStatusSignalementRepository historiqueRepository;

    /**
     * Crée un nouvel historique lors d'un changement de statut
     */
    public HistoriqueStatusSignalement createHistorique(Long idSignalement, Long idStatus, Long idUtilisateur, String commentaire) {
        HistoriqueStatusSignalement historique = new HistoriqueStatusSignalement(
            idSignalement, 
            idStatus, 
            idUtilisateur, 
            commentaire
        );
        return historiqueRepository.save(historique);
    }

    /**
     * Récupère tous les historiques
     */
    public List<HistoriqueStatusSignalement> getAllHistoriques() {
        return historiqueRepository.findAll();
    }

    /**
     * Récupère tous les historiques d'un signalement
     */
    public List<HistoriqueStatusSignalement> getHistoriquesBySignalement(Long idSignalement) {
        return historiqueRepository.findByIdSignalementOrderByCreatedAtDesc(idSignalement);
    }

    /**
     * Récupère le dernier historique d'un signalement
     */
    public Optional<HistoriqueStatusSignalement> getLatestHistorique(Long idSignalement) {
        return historiqueRepository.findLatestByIdSignalement(idSignalement);
    }

    /**
     * Récupère la date de dernière mise à jour d'un signalement
     */
    public Optional<LocalDateTime> getLatestUpdateDate(Long idSignalement) {
        return historiqueRepository.findLatestCreatedAtByIdSignalement(idSignalement);
    }
}
