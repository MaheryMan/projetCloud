package com.projetCloud.app.signalements;

import com.projetCloud.app.historiques.HistoriqueStatusSignalementService;
import com.projetCloud.app.status.Status;
import com.projetCloud.app.status.StatusService;
import com.projetCloud.app.typesSignalement.TypeSignalement;
import com.projetCloud.app.typesSignalement.TypeSignalementService;
import com.projetCloud.app.utilisateurs.Utilisateur;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrôleur pour la gestion des signalements
 */
@RestController
@RequestMapping("/api/signalements")
@Tag(name = "Signalements", description = "API pour la gestion des signalements")
public class SignalementController {

    @Autowired
    private SignalementService signalementService;

    @Autowired
    private StatusService statusService;

    @Autowired
    private TypeSignalementService typeSignalementService;

    @Autowired
    private HistoriqueStatusSignalementService historiqueService;

    @GetMapping
    @Operation(summary = "Récupérer tous les signalements", description = "Retourne la liste de tous les signalements")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des signalements récupérée avec succès",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = Signalement.class)))
    })
    public List<Signalement> getAllSignalements() {
        return signalementService.findAll();
    }

    @GetMapping("/recent")
    @Operation(summary = "Récupérer les signalements récents", description = "Retourne les 10 derniers signalements")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des signalements récents récupérée avec succès",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = Signalement.class)))
    })
    public List<Signalement> getRecentSignalements() {
        List<Signalement> all = signalementService.findAll();
        // Return last 10, assuming sorted by date desc
        return all.size() > 10 ? all.subList(0, 10) : all;
    }

    @GetMapping("/stats")
    @Operation(summary = "Récupérer les statistiques des signalements", description = "Retourne les statistiques globales avec calcul d'avancement correct")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistiques récupérées avec succès",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getStats() {
        List<Signalement> all = signalementService.findAll();
        
        // Compter par statut (4=Nouveau, 5=En cours, 6=Terminé)
        // Ignorer les statuts utilisateurs (1, 2, 3)
        long nouveau = all.stream().filter(s -> s.getIdStatus() != null && s.getIdStatus() == 4).count();
        long enCours = all.stream().filter(s -> s.getIdStatus() != null && s.getIdStatus() == 5).count();
        long termine = all.stream().filter(s -> s.getIdStatus() != null && s.getIdStatus() == 6).count();
        
        // Total = seulement les signalements avec statuts valides (4, 5, 6)
        long total = nouveau + enCours + termine;
        long totalSignalements = all.size(); // Total incluant les mal catégorisés
        
        // Calculer surface totale
        double surfaceTotal = all.stream()
            .map(Signalement::getSurfaceM2)
            .filter(s -> s != null)
            .mapToDouble(BigDecimal::doubleValue)
            .sum();
        
        // Calculer chiffre d'affaire (somme de tous les budgets)
        double chiffreAffaire = all.stream()
            .map(Signalement::getBudget)
            .filter(b -> b != null)
            .mapToDouble(BigDecimal::doubleValue)
            .sum();
        
        // Calculer avancement: formule pondérée
        // Nouveau = 0%, En cours = 50%, Terminé = 100%
        // Avancement = ((En cours × 0.5) + Terminé) / Total × 100
        int avancement = 0;
        if (total > 0) {
            double avancementDouble = ((enCours * 0.5) + termine) / (double) total * 100.0;
            avancement = (int) Math.round(avancementDouble);
        }
        
        System.out.println("Stats Debug - Total valides: " + total + 
                          ", Nouveau: " + nouveau + ", En cours: " + enCours + ", Terminé: " + termine + 
                          ", Calcul: ((" + enCours + " * 0.5) + " + termine + ") / " + total + " * 100 = " + 
                          ((enCours * 0.5) + termine) + " / " + total + " * 100 = " + avancement + "%");
        
        return ResponseEntity.ok(Map.of(
            "totalSignalements", totalSignalements,
            "nouveau", nouveau,
            "enCours", enCours,
            "termine", termine,
            "surfaceTotal", surfaceTotal,
            "chiffreAffaire", chiffreAffaire,
            "avancement", avancement
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un signalement par ID", description = "Retourne un signalement spécifique selon son identifiant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signalement trouvé",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = Signalement.class))),
        @ApiResponse(responseCode = "404", description = "Signalement non trouvé",
                    content = @Content)
    })
    public ResponseEntity<Signalement> getSignalementById(@Parameter(description = "ID du signalement") @PathVariable Long id) {
        Optional<Signalement> signalement = signalementService.findById(id);
        if (signalement.isPresent()) {
            return ResponseEntity.ok(signalement.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Créer un nouveau signalement", description = "Crée un nouveau signalement avec les informations fournies")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signalement créé avec succès",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = Signalement.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou utilisateur/statut non trouvé",
                    content = @Content)
    })
    public ResponseEntity<Signalement> createSignalement(
            @Parameter(description = "Données du signalement à créer", required = true)
            @RequestBody SignalementRequest request,
            HttpServletRequest httpServletRequest) {
        Object currentUserAttr = httpServletRequest.getAttribute("currentUser");
        Utilisateur currentUser = currentUserAttr instanceof Utilisateur ? (Utilisateur) currentUserAttr : null;
        Optional<Utilisateur> utilisateur = currentUser != null ? Optional.of(currentUser) : Optional.empty();
        Optional<TypeSignalement> typeSignalement = typeSignalementService.findById(request.getIdTypeSignalement());
        Optional<Status> status = statusService.findByCode("REPORT_NOUVEAU");

        if (utilisateur.isPresent() && typeSignalement.isPresent() && status.isPresent()) {
            Signalement signalement = new Signalement(
                request.getLatitude(),
                request.getLongitude(),
                request.getSurfaceM2(),
                request.getBudget(),
                request.getDescription(),
                request.getPhotoUrl(),
                request.getIdEntreprise(),
                status.get().getId(),
                typeSignalement.get(),
                utilisateur.get()
            );
            Signalement saved = signalementService.save(signalement);
            
            // Créer un historique pour le statut initial
            historiqueService.createHistorique(
                saved.getId(), 
                saved.getIdStatus(), 
                utilisateur.get().getId(), 
                "Création du signalement"
            );
            
            return ResponseEntity.ok(saved);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un signalement", description = "Met à jour un signalement existant avec les informations fournies")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signalement mis à jour avec succès",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = Signalement.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou statut/type non trouvé",
                    content = @Content),
        @ApiResponse(responseCode = "404", description = "Signalement non trouvé",
                    content = @Content)
    })
    public ResponseEntity<Signalement> updateSignalement(
            @Parameter(description = "ID du signalement") @PathVariable Long id,
            @RequestBody SignalementRequest request,
            HttpServletRequest httpServletRequest) {
        Optional<Signalement> signalementOpt = signalementService.findById(id);
        if (signalementOpt.isPresent()) {
            Signalement signalement = signalementOpt.get();
            Long oldStatus = signalement.getIdStatus();

            if (request.getLatitude() != null) {
                signalement.setLatitude(request.getLatitude());
            }
            if (request.getLongitude() != null) {
                signalement.setLongitude(request.getLongitude());
            }
            if (request.getSurfaceM2() != null) {
                signalement.setSurfaceM2(request.getSurfaceM2());
            }
            if (request.getBudget() != null) {
                signalement.setBudget(request.getBudget());
            }
            if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
                signalement.setDescription(request.getDescription());
            }
            if (request.getPhotoUrl() != null) {
                signalement.setPhotoUrl(request.getPhotoUrl());
            }
            if (request.getIdEntreprise() != null) {
                signalement.setIdEntreprise(request.getIdEntreprise());
            }
            if (request.getIdStatus() != null) {
                Optional<Status> status = statusService.findById(request.getIdStatus());
                if (status.isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                signalement.setIdStatus(request.getIdStatus());
            }
            if (request.getIdTypeSignalement() != null) {
                Optional<TypeSignalement> typeSignalement = typeSignalementService.findById(request.getIdTypeSignalement());
                if (typeSignalement.isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                signalement.setTypeSignalement(typeSignalement.get());
            }

            Signalement saved = signalementService.save(signalement);

            // Créer un historique si le statut a changé
            if (request.getIdStatus() != null && !request.getIdStatus().equals(oldStatus)) {
                Object currentUserAttr = httpServletRequest.getAttribute("currentUser");
                Long userId = currentUserAttr instanceof Utilisateur
                        ? ((Utilisateur) currentUserAttr).getId()
                        : signalement.getUtilisateur().getId();

                historiqueService.createHistorique(
                        saved.getId(),
                        saved.getIdStatus(),
                        userId,
                        "Mise à jour du statut"
                );
            }

            return ResponseEntity.ok(saved);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un signalement", description = "Supprime un signalement selon son identifiant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Signalement supprimé avec succès",
                    content = @Content),
        @ApiResponse(responseCode = "404", description = "Signalement non trouvé",
                    content = @Content)
    })
    public ResponseEntity<Void> deleteSignalement(@Parameter(description = "ID du signalement") @PathVariable Long id) {
        if (signalementService.findById(id).isPresent()) {
            signalementService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Classe interne pour la requête
    @Schema(description = "Requête de création/modification d'un signalement", example = """
            {
              "latitude": -18.8792,
              "longitude": 47.5079,
              "surfaceM2": 100.50,
              "budget": 150000.00,
              "description": "Route endommagée nécessitant réparation",
              "photoUrl": "https://example.com/photo.jpg",
              "idTypeSignalement": 1,
              "idStatus": 1,
              "idEntreprise": 1,
              "idUtilisateur": 1
            }
            """)
    public static class SignalementRequest {

        @Schema(description = "Latitude du signalement", example = "-18.8792", required = true, format = "double")
        private BigDecimal latitude;

        @Schema(description = "Longitude du signalement", example = "47.5079", required = true, format = "double")
        private BigDecimal longitude;

        @Schema(description = "Surface en mètres carrés", example = "100.50", format = "double")
        private BigDecimal surfaceM2;

        @Schema(description = "Budget estimé", example = "150000.00", format = "double")
        private BigDecimal budget;

        @Schema(description = "Description du signalement", example = "Route endommagée nécessitant réparation", required = true)
        private String description;

        @Schema(description = "URL de la photo", example = "https://example.com/photo.jpg")
        private String photoUrl;

        @Schema(description = "ID du type de signalement", example = "1", required = true, format = "int64")
        private Long idTypeSignalement;

        @Schema(description = "ID du statut du signalement", example = "1", required = true, format = "int64")
        private Long idStatus;

        @Schema(description = "ID de l'entreprise", example = "1", format = "int64")
        private Long idEntreprise;

        @Schema(description = "ID de l'utilisateur", example = "1", required = true, format = "int64")
        private Long idUtilisateur;

        public BigDecimal getLatitude() {
            return latitude;
        }

        public void setLatitude(BigDecimal latitude) {
            this.latitude = latitude;
        }

        public BigDecimal getLongitude() {
            return longitude;
        }

        public void setLongitude(BigDecimal longitude) {
            this.longitude = longitude;
        }

        public BigDecimal getSurfaceM2() {
            return surfaceM2;
        }

        public void setSurfaceM2(BigDecimal surfaceM2) {
            this.surfaceM2 = surfaceM2;
        }

        public BigDecimal getBudget() {
            return budget;
        }

        public void setBudget(BigDecimal budget) {
            this.budget = budget;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }

        public void setPhotoUrl(String photoUrl) {
            this.photoUrl = photoUrl;
        }

        public Long getIdTypeSignalement() {
            return idTypeSignalement;
        }

        public void setIdTypeSignalement(Long idTypeSignalement) {
            this.idTypeSignalement = idTypeSignalement;
        }

        public Long getIdStatus() {
            return idStatus;
        }

        public void setIdStatus(Long idStatus) {
            this.idStatus = idStatus;
        }

        public Long getIdEntreprise() {
            return idEntreprise;
        }

        public void setIdEntreprise(Long idEntreprise) {
            this.idEntreprise = idEntreprise;
        }

        public Long getIdUtilisateur() {
            return idUtilisateur;
        }

        public void setIdUtilisateur(Long idUtilisateur) {
            this.idUtilisateur = idUtilisateur;
        }
    }
}