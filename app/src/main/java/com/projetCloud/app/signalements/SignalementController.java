package com.projetCloud.app.signalements;

import com.projetCloud.app.historiques.HistoriqueStatusSignalementService;
import com.projetCloud.app.status.Status;
import com.projetCloud.app.status.StatusService;
import com.projetCloud.app.typesSignalement.TypeSignalement;
import com.projetCloud.app.typesSignalement.TypeSignalementService;
import com.projetCloud.app.utilisateurs.Utilisateur;
import com.projetCloud.app.utilisateurs.UtilisateurService;
import com.projetCloud.app.photos.Photo;
import com.projetCloud.app.photos.PhotoService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private PhotoService photoService;

    @Autowired
    private UtilisateurService utilisateurService;
    
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
        
        // Récupérer l'utilisateur (connecté ou par ID fourni dans la requête)
        Optional<Utilisateur> utilisateur = Optional.empty();
        if (currentUser != null) {
            utilisateur = Optional.of(currentUser);
        } else if (request.getIdUtilisateur() != null) {
            utilisateur = utilisateurService.findById(request.getIdUtilisateur());
        }
        
        Optional<TypeSignalement> typeSignalement = typeSignalementService.findById(request.getIdTypeSignalement());
        Optional<Status> status = statusService.findByCode("REPORT001"); // Nouveau

        // Log pour debug
        System.out.println("=== DEBUG CREATE SIGNALEMENT ===");
        System.out.println("Utilisateur present: " + utilisateur.isPresent());
        System.out.println("TypeSignalement present: " + typeSignalement.isPresent() + " (ID: " + request.getIdTypeSignalement() + ")");
        System.out.println("Status present: " + status.isPresent());
        if (!utilisateur.isPresent()) {
            System.out.println("ERREUR: Utilisateur non trouvé (ID: " + request.getIdUtilisateur() + ")");
        }
        if (!typeSignalement.isPresent()) {
            System.out.println("ERREUR: TypeSignalement non trouvé (ID: " + request.getIdTypeSignalement() + ")");
        }
        if (!status.isPresent()) {
            System.out.println("ERREUR: Status 'REPORT001' non trouvé");
        }
        System.out.println("================================");

        if (utilisateur.isPresent() && typeSignalement.isPresent() && status.isPresent()) {
            Signalement signalement = new Signalement(
                request.getLatitude(),
                request.getLongitude(),
                request.getSurfaceM2(),
                request.getBudget(),
                request.getNiveau(),
                request.getDescription(),
                request.getIdEntreprise(),
                status.get().getId(),
                typeSignalement.get(),
                utilisateur.get()
            );
            
            // Gestion de la photo si une URL est fournie
            if (request.getPhotoUrl() != null && !request.getPhotoUrl().trim().isEmpty()) {
                try {
                    // Créer une nouvelle photo SANS la sauvegarder (pas de cascade issue)
                    Photo photo = new Photo(
                        request.getPhotoUrl().trim(),
                        null,
                        null,
                        "image/jpeg"
                    );
                    signalement.addPhoto(photo);
                } catch (Exception e) {
                    // Log l'erreur mais continue sans photo
                    System.err.println("Erreur lors de la création de la photo: " + e.getMessage());
                }
            }
            
            // Gestion des photos multiples (si fournies)
            if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
                for (String photoUrl : request.getPhotoUrls()) {
                    if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                        try {
                            // Créer une nouvelle photo SANS la sauvegarder
                            Photo photo = new Photo(
                                photoUrl.trim(),
                                null,
                                null,
                                "image/jpeg"
                            );
                            signalement.addPhoto(photo);
                        } catch (Exception e) {
                            System.err.println("Erreur lors de la création d'une photo multiple: " + e.getMessage());
                        }
                    }
                }
            }
            
            Signalement saved = signalementService.save(signalement);
            
            // Créer un historique pour le statut initial
            LocalDateTime dateModif = parseDate(request.getDateModificationStatus());
            String commentaire = (request.getCommentaireStatus() != null && !request.getCommentaireStatus().trim().isEmpty())
                    ? "Création du signalement: " + request.getCommentaireStatus().trim()
                    : "Création du signalement";
            
            if (dateModif != null) {
                historiqueService.createHistorique(
                    saved.getId(), 
                    saved.getIdStatus(), 
                    utilisateur.get().getId(), 
                    commentaire,
                    dateModif
                );
            } else {
                historiqueService.createHistorique(
                    saved.getId(), 
                    saved.getIdStatus(), 
                    utilisateur.get().getId(), 
                    commentaire
                );
            }
            
            return ResponseEntity.ok(saved);
        } else {
            // Créer un message d'erreur détaillé
            String errorMessage = "Erreur de validation: ";
            if (!utilisateur.isPresent()) {
                errorMessage += "Utilisateur non trouvé (ID: " + request.getIdUtilisateur() + "). ";
            }
            if (!typeSignalement.isPresent()) {
                errorMessage += "Type de signalement non trouvé (ID: " + request.getIdTypeSignalement() + "). ";
            }
            if (!status.isPresent()) {
                errorMessage += "Statut 'REPORT001' (Nouveau) non trouvé dans la base de données. ";
            }
            System.err.println(errorMessage);
            return ResponseEntity.badRequest().body(null);
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
            if (request.getNiveau() != null) {
                signalement.setNiveau(request.getNiveau());
            }
            
            // Gestion de la photo
            if (request.getPhotoUrl() != null) {
                if (request.getPhotoUrl().trim().isEmpty()) {
                    // Supprimer toutes les photos si URL vide
                    signalement.getPhotos().clear();
                } else {
                    try {
                        // Remplacer toutes les photos par la nouvelle
                        signalement.getPhotos().clear();
                        Photo photo = photoService.findOrCreateByUrl(
                            request.getPhotoUrl().trim(), 
                            null, 
                            "image/jpeg"
                        );
                        signalement.addPhoto(photo);
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la mise à jour de la photo: " + e.getMessage());
                    }
                }
            }
            
            // Gestion des photos multiples (remplace toutes les photos existantes)
            if (request.getPhotoUrls() != null) {
                signalement.getPhotos().clear();
                for (String photoUrl : request.getPhotoUrls()) {
                    if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                        try {
                            Photo photo = photoService.findOrCreateByUrl(
                                photoUrl.trim(), 
                                null, 
                                "image/jpeg"
                            );
                            signalement.addPhoto(photo);
                        } catch (Exception e) {
                            System.err.println("Erreur lors de l'ajout d'une photo multiple: " + e.getMessage());
                        }
                    }
                }
            }
            
            // Marquer pour synchronisation Firebase si le signalement a un firebaseId
            if (signalement.getFirebaseId() != null) {
                signalement.setNeedsFirebaseSync(true);
            }
            
            Signalement saved = signalementService.save(signalement);

            // Créer un historique si le statut a changé
            if (request.getIdStatus() != null && !request.getIdStatus().equals(oldStatus)) {
                Object currentUserAttr = httpServletRequest.getAttribute("currentUser");
                Long userId = currentUserAttr instanceof Utilisateur
                        ? ((Utilisateur) currentUserAttr).getId()
                        : signalement.getUtilisateur().getId();

                LocalDateTime dateModif = parseDate(request.getDateModificationStatus());
                String commentaire = (request.getCommentaireStatus() != null && !request.getCommentaireStatus().trim().isEmpty())
                        ? "Mise à jour status: " + request.getCommentaireStatus().trim()
                        : "Mise à jour du statut";
                
                if (dateModif != null) {
                    historiqueService.createHistorique(
                            saved.getId(),
                            saved.getIdStatus(),
                            userId,
                            commentaire,
                            dateModif
                    );
                } else {
                    historiqueService.createHistorique(
                            saved.getId(),
                            saved.getIdStatus(),
                            userId,
                            commentaire
                    );
                }
            }

            return ResponseEntity.ok(saved);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Valider un signalement", description = "Le manager valide un signalement 'Créé' et le passe à 'Nouveau' avec optionnellement entreprise et budget")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signalement validé avec succès",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = Signalement.class))),
        @ApiResponse(responseCode = "404", description = "Signalement non trouvé",
                    content = @Content),
        @ApiResponse(responseCode = "400", description = "Signalement n'est pas au status 'Créé'",
                    content = @Content)
    })
    public ResponseEntity<?> validateSignalement(
            @Parameter(description = "ID du signalement") @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> validationData) {
        
        Optional<Signalement> signalementOpt = signalementService.findById(id);
        if (signalementOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Signalement signalement = signalementOpt.get();
        
        // Vérifier que le signalement est au status "Créé" (id 8)
        if (signalement.getIdStatus() != 8L) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Le signalement n'est pas au status 'Créé'"));
        }

        // Passer à "Nouveau" (id 4)
        signalement.setIdStatus(4L);
        
        // Optionnel: ajouter entreprise et budget
        if (validationData != null) {
            if (validationData.containsKey("idEntreprise")) {
                Long idEntreprise = ((Number) validationData.get("idEntreprise")).longValue();
                signalement.setIdEntreprise(idEntreprise);
                
                // Si entreprise assignée, passer à "En cours" (id 5)
                signalement.setIdStatus(5L);
            }
            
            if (validationData.containsKey("budget")) {
                BigDecimal budget = new BigDecimal(validationData.get("budget").toString());
                signalement.setBudget(budget);
            }
            
            if (validationData.containsKey("surfaceM2")) {
                BigDecimal surface = new BigDecimal(validationData.get("surfaceM2").toString());
                signalement.setSurfaceM2(surface);
            }
        }

        // Marquer pour synchronisation Firebase
        if (signalement.getFirebaseId() != null) {
            signalement.setNeedsFirebaseSync(true);
        }

        Signalement saved = signalementService.save(signalement);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Signalement validé avec succès",
                "signalement", saved
        ));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Rejeter un signalement", description = "Le manager rejette un signalement et le passe au status 'Rejeté'")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signalement rejeté avec succès",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = Signalement.class))),
        @ApiResponse(responseCode = "404", description = "Signalement non trouvé",
                    content = @Content)
    })
    public ResponseEntity<?> rejectSignalement(@Parameter(description = "ID du signalement") @PathVariable Long id) {
        Optional<Signalement> signalementOpt = signalementService.findById(id);
        if (signalementOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Signalement signalement = signalementOpt.get();
        
        // Passer à "Annulé/Rejeté" (id 7 si existe, sinon utiliser 4 avec note)
        signalement.setIdStatus(7L); // Vous devrez peut-être ajuster selon vos status

        // Marquer pour synchronisation Firebase
        if (signalement.getFirebaseId() != null) {
            signalement.setNeedsFirebaseSync(true);
        }

        Signalement saved = signalementService.save(signalement);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Signalement rejeté",
                "signalement", saved
        ));
    }

    /**
     * Méthode utilitaire pour parser une date ISO
     */
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            System.err.println("Erreur de parsing de la date: " + dateStr + " - " + e.getMessage());
            return null;
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

        @Schema(description = "Liste des URLs des photos", example = "[\"https://example.com/photo1.jpg\", \"https://example.com/photo2.jpg\"]")
        private List<String> photoUrls;

        @Schema(description = "ID du type de signalement", example = "1", required = true, format = "int64")
        private Long idTypeSignalement;

        @Schema(description = "ID du statut du signalement", example = "1", required = true, format = "int64")
        private Long idStatus;

        @Schema(description = "ID de l'entreprise", example = "1", format = "int64")
        private Long idEntreprise;

        @Schema(description = "ID de l'utilisateur", example = "1", required = true, format = "int64")
        private Long idUtilisateur;

        @Schema(description = "Date de modification du statut (format ISO: yyyy-MM-dd'T'HH:mm:ss)", example = "2024-01-15T14:30:00")
        private String dateModificationStatus;

        @Schema(description = "Commentaire personnalisé pour le changement de statut", example = "Validation par le manager")
        private String commentaireStatus;

        @Schema(description = "Niveau d'urgence ou de priorité", example = "3", format = "int32")
        private Integer niveau;

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

        public List<String> getPhotoUrls() {
            return photoUrls;
        }

        public void setPhotoUrls(List<String> photoUrls) {
            this.photoUrls = photoUrls;
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

        public String getDateModificationStatus() {
            return dateModificationStatus;
        }

        public void setDateModificationStatus(String dateModificationStatus) {
            this.dateModificationStatus = dateModificationStatus;
        }

        public String getCommentaireStatus() {
            return commentaireStatus;
        }

        public void setCommentaireStatus(String commentaireStatus) {
            this.commentaireStatus = commentaireStatus;
        }

        public Integer getNiveau() {
            return niveau;
        }

        public void setNiveau(Integer niveau) {
            this.niveau = niveau;
        }
    }
}