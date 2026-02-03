package com.projetCloud.app.historiques;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historiques")
@CrossOrigin(origins = "*")
public class HistoriqueStatusSignalementController {

    @Autowired
    private HistoriqueStatusSignalementService historiqueService;

    /**
     * Récupère tous les historiques
     */
    @GetMapping
    public ResponseEntity<List<HistoriqueStatusSignalement>> getAllHistoriques() {
        List<HistoriqueStatusSignalement> historiques = historiqueService.getAllHistoriques();
        return ResponseEntity.ok(historiques);
    }

    /**
     * Récupère l'historique d'un signalement spécifique
     */
    @GetMapping("/signalement/{idSignalement}")
    public ResponseEntity<List<HistoriqueStatusSignalement>> getHistoriquesBySignalement(
            @PathVariable Long idSignalement) {
        List<HistoriqueStatusSignalement> historiques = historiqueService.getHistoriquesBySignalement(idSignalement);
        return ResponseEntity.ok(historiques);
    }

    /**
     * Récupère le dernier historique d'un signalement
     */
    @GetMapping("/signalement/{idSignalement}/latest")
    public ResponseEntity<?> getLatestHistorique(@PathVariable Long idSignalement) {
        return historiqueService.getLatestHistorique(idSignalement)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
