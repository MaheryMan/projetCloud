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

@RestController
@RequestMapping("/api/signalements")
public class SignalementController {

    @Autowired
    private SignalementService signalementService;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private NiveauTravailService niveauTravailService;

    @GetMapping
    public List<Signalement> getAllSignalements() {
        return signalementService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Signalement> getSignalementById(@PathVariable Long id) {
        Optional<Signalement> signalement = signalementService.findById(id);
        if (signalement.isPresent()) {
            return ResponseEntity.ok(signalement.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Signalement> createSignalement(@RequestBody SignalementRequest request) {
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
    public ResponseEntity<Signalement> updateSignalement(@PathVariable Long id, @RequestBody SignalementRequest request) {
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
    public ResponseEntity<Void> deleteSignalement(@PathVariable Long id) {
        if (signalementService.findById(id).isPresent()) {
            signalementService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Classe interne pour la requête
    public static class SignalementRequest {
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal surfaceM2;
        private String description;
        private Long idNiveauTravail;
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