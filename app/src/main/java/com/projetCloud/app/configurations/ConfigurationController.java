package com.projetCloud.app.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur pour gérer les configurations système
 * Paramètres: tentatives_max, duree_session_minutes, duree_blocage_minutes
 */
@RestController
@RequestMapping("/api/configurations")
@CrossOrigin(origins = "*")
public class ConfigurationController {

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Récupérer toutes les configurations
     * GET /api/configurations
     */
    @GetMapping
    public ResponseEntity<List<Configuration>> getAllConfigurations() {
        List<Configuration> configurations = configurationService.getAllConfigurations();
        return ResponseEntity.ok(configurations);
    }

    /**
     * Récupérer une configuration par sa clé
     * GET /api/configurations/{cle}
     */
    @GetMapping("/{cle}")
    public ResponseEntity<Configuration> getConfigurationByCle(@PathVariable String cle) {
        return configurationService.getConfigurationByCle(cle)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Créer ou mettre à jour une configuration
     * POST /api/configurations
     * 
     * Body example:
     * {
     *   "cle": "tentatives_max",
     *   "valeur": "5",
     *   "description": "Nombre maximum de tentatives de connexion avant blocage"
     * }
     */
    @PostMapping
    public ResponseEntity<Configuration> createOrUpdateConfiguration(@RequestBody Configuration configuration) {
        Configuration savedConfig = configurationService.saveOrUpdateConfiguration(configuration);
        return ResponseEntity.ok(savedConfig);
    }

    /**
     * Mettre à jour une configuration existante
     * PUT /api/configurations/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Configuration> updateConfiguration(
            @PathVariable Long id, 
            @RequestBody Configuration configuration) {
        configuration.setId(id);
        Configuration updatedConfig = configurationService.saveOrUpdateConfiguration(configuration);
        return ResponseEntity.ok(updatedConfig);
    }

    /**
     * Supprimer une configuration
     * DELETE /api/configurations/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable Long id) {
        configurationService.deleteConfiguration(id);
        return ResponseEntity.noContent().build();
    }
}
