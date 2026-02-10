package com.projetCloud.app.prixForfaitaire;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/prix-forfaitaire")
@CrossOrigin(origins = "*")
public class PrixForfaitaireController {

    @Autowired
    private PrixForfaitaireService prixForfaitaireService;

    /**
     * Récupère tous les prix forfaitaires
     */
    @GetMapping
    public ResponseEntity<List<PrixForfaitaire>> getAllPrix() {
        List<PrixForfaitaire> prix = prixForfaitaireService.findAll();
        return ResponseEntity.ok(prix);
    }

    /**
     * Récupère le prix forfaitaire actif
     */
    @GetMapping("/actif")
    public ResponseEntity<?> getActivePrix() {
        Optional<PrixForfaitaire> prixOpt = prixForfaitaireService.findActivePrix();
        
        if (prixOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Aucun prix forfaitaire actif trouvé");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        return ResponseEntity.ok(prixOpt.get());
    }

    /**
     * Récupère un prix forfaitaire par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPrixById(@PathVariable Long id) {
        Optional<PrixForfaitaire> prixOpt = prixForfaitaireService.findById(id);
        
        if (prixOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Prix forfaitaire non trouvé");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        return ResponseEntity.ok(prixOpt.get());
    }

    /**
     * Crée un nouveau prix forfaitaire
     */
    @PostMapping
    public ResponseEntity<?> createPrix(@RequestBody PrixForfaitaireDTO prixDTO) {
        try {
            // Validation
            if (prixDTO.getPrixParMetreCarre() == null || prixDTO.getPrixParMetreCarre().compareTo(BigDecimal.ZERO) <= 0) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Le prix par mètre carré doit être supérieur à 0");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            if (prixDTO.getMultiplicateurNiveau() == null || prixDTO.getMultiplicateurNiveau().compareTo(BigDecimal.ZERO) <= 0) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Le multiplicateur de niveau doit être supérieur à 0");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            PrixForfaitaire nouveauPrix = prixForfaitaireService.createNewPrix(
                prixDTO.getPrixParMetreCarre(), 
                prixDTO.getMultiplicateurNiveau()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(nouveauPrix);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Erreur lors de la création du prix: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Met à jour le prix forfaitaire actif
     */
    @PutMapping
    public ResponseEntity<?> updatePrix(@RequestBody PrixForfaitaireDTO prixDTO) {
        try {
            // Validation
            if (prixDTO.getPrixParMetreCarre() == null || prixDTO.getPrixParMetreCarre().compareTo(BigDecimal.ZERO) <= 0) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Le prix par mètre carré doit être supérieur à 0");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            if (prixDTO.getMultiplicateurNiveau() == null || prixDTO.getMultiplicateurNiveau().compareTo(BigDecimal.ZERO) <= 0) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Le multiplicateur de niveau doit être supérieur à 0");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            PrixForfaitaire nouveauPrix = prixForfaitaireService.updatePrix(
                prixDTO.getPrixParMetreCarre(), 
                prixDTO.getMultiplicateurNiveau()
            );
            
            return ResponseEntity.ok(nouveauPrix);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Erreur lors de la mise à jour du prix: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Calcule le budget pour un signalement
     */
    @PostMapping("/calculer-budget")
    public ResponseEntity<?> calculateBudget(@RequestBody BudgetCalculationDTO calculationDTO) {
        try {
            if (calculationDTO.getSurface() == null || calculationDTO.getSurface().compareTo(BigDecimal.ZERO) <= 0) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "La surface doit être supérieure à 0");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            if (calculationDTO.getNiveauUrgence() == null || calculationDTO.getNiveauUrgence() < 0) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Le niveau d'urgence doit être supérieur ou égal à 0");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            BigDecimal budget = prixForfaitaireService.calculerBudget(
                calculationDTO.getSurface(), 
                calculationDTO.getNiveauUrgence()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("budget", budget);
            response.put("surface", calculationDTO.getSurface());
            response.put("niveauUrgence", calculationDTO.getNiveauUrgence());
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Erreur lors du calcul du budget: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Désactive un prix forfaitaire
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivatePrix(@PathVariable Long id) {
        try {
            prixForfaitaireService.deactivatePrix(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Prix forfaitaire désactivé avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Erreur lors de la désactivation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * DTO pour la création/mise à jour du prix forfaitaire
     */
    public static class PrixForfaitaireDTO {
        private BigDecimal prixParMetreCarre;
        private BigDecimal multiplicateurNiveau;

        public BigDecimal getPrixParMetreCarre() {
            return prixParMetreCarre;
        }

        public void setPrixParMetreCarre(BigDecimal prixParMetreCarre) {
            this.prixParMetreCarre = prixParMetreCarre;
        }

        public BigDecimal getMultiplicateurNiveau() {
            return multiplicateurNiveau;
        }

        public void setMultiplicateurNiveau(BigDecimal multiplicateurNiveau) {
            this.multiplicateurNiveau = multiplicateurNiveau;
        }
    }

    /**
     * DTO pour le calcul du budget
     */
    public static class BudgetCalculationDTO {
        private BigDecimal surface;
        private Integer niveauUrgence;

        public BigDecimal getSurface() {
            return surface;
        }

        public void setSurface(BigDecimal surface) {
            this.surface = surface;
        }

        public Integer getNiveauUrgence() {
            return niveauUrgence;
        }

        public void setNiveauUrgence(Integer niveauUrgence) {
            this.niveauUrgence = niveauUrgence;
        }
    }
}
