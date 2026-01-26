package com.projetCloud.app.signalements;

import com.projetCloud.app.niveauTravaux.NiveauTravail;
import com.projetCloud.app.niveauTravaux.NiveauTravailService;
import com.projetCloud.app.utilisateurs.Utilisateur;
import com.projetCloud.app.utilisateurs.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/signalements")
@Tag(name = "Signalements", description = "API pour la gestion des signalements")
public class SignalementController {

    @Autowired
    private SignalementService signalementService;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private NiveauTravailService niveauTravailService;

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
        @ApiResponse(responseCode = "400", description = "Données invalides ou utilisateur/niveau de travail non trouvé",
                    content = @Content)
    })
    public ResponseEntity<Signalement> createSignalement(
            @Parameter(description = "Données du signalement à créer", required = true)
            @RequestBody SignalementRequest request) {
        Optional<Utilisateur> utilisateur = utilisateurService.findById(request.getIdUtilisateur());
        Optional<NiveauTravail> niveauTravail = niveauTravailService.findById(request.getIdNiveauTravail());

        if (utilisateur.isPresent() && niveauTravail.isPresent()) {
            Signalement signalement = new Signalement(
                request.getLatitude(),
                request.getLongitude(),
                request.getSurfaceM2(),
                request.getDescription(),
                niveauTravail.get(),
                utilisateur.get()
            );
            return ResponseEntity.ok(signalementService.save(signalement));
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un signalement", description = "Met à jour les informations d'un signalement existant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signalement mis à jour avec succès",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = Signalement.class))),
        @ApiResponse(responseCode = "404", description = "Signalement non trouvé",
                    content = @Content)
    })
    public ResponseEntity<Signalement> updateSignalement(@Parameter(description = "ID du signalement") @PathVariable Long id, @RequestBody SignalementRequest request) {
        Optional<Signalement> signalementOpt = signalementService.findById(id);
        if (signalementOpt.isPresent()) {
            Signalement signalement = signalementOpt.get();
            signalement.setLatitude(request.getLatitude());
            signalement.setLongitude(request.getLongitude());
            signalement.setSurfaceM2(request.getSurfaceM2());
            signalement.setDescription(request.getDescription());
            // niveauTravail and utilisateur not updated in this example
            return ResponseEntity.ok(signalementService.save(signalement));
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
              "description": "Route endommagée nécessitant réparation",
              "idNiveauTravail": 1,
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

        @Schema(description = "Description du signalement", example = "Route endommagée nécessitant réparation", required = true)
        private String description;

        @Schema(description = "ID du niveau de travail", example = "1", required = true, format = "int64")
        private Long idNiveauTravail;

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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Long getIdNiveauTravail() {
            return idNiveauTravail;
        }

        public void setIdNiveauTravail(Long idNiveauTravail) {
            this.idNiveauTravail = idNiveauTravail;
        }

        public Long getIdUtilisateur() {
            return idUtilisateur;
        }

        public void setIdUtilisateur(Long idUtilisateur) {
            this.idUtilisateur = idUtilisateur;
        }
    }
}