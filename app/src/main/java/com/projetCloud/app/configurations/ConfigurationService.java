package com.projetCloud.app.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service pour gérer les configurations système
 */
@Service
public class ConfigurationService {

    @Autowired
    private ConfigurationRepository configurationRepository;

    /**
     * Récupère la valeur d'une configuration
     */
    public String getValue(String key, String defaultValue) {
        Optional<Configuration> configOpt = configurationRepository.findByCle(key);
        if (configOpt.isPresent()) {
            return configOpt.get().getValeur();
        } else {
            return defaultValue;
        }
    }

    /**
     * Récupère l'email du manager par défaut
     */
    public String getDefaultManagerEmail() {
        return getValue("default_manager_email", "manager@projetcloud.com");
    }

    /**
     * Récupère toutes les configurations
     */
    public List<Configuration> getAllConfigurations() {
        return configurationRepository.findAll();
    }

    /**
     * Récupère une configuration par sa clé
     */
    public Optional<Configuration> getConfigurationByCle(String cle) {
        return configurationRepository.findByCle(cle);
    }

    /**
     * Sauvegarde ou met à jour une configuration
     */
    public Configuration saveOrUpdateConfiguration(Configuration configuration) {
        Optional<Configuration> existingConfig = configurationRepository.findByCle(configuration.getCle());
        
        if (existingConfig.isPresent()) {
            // Mise à jour de la configuration existante
            Configuration config = existingConfig.get();
            config.setValeur(configuration.getValeur());
            if (configuration.getDescription() != null) {
                config.setDescription(configuration.getDescription());
            }
            config.setUpdatedAt(LocalDateTime.now());
            return configurationRepository.save(config);
        } else {
            // Nouvelle configuration
            configuration.setCreatedAt(LocalDateTime.now());
            configuration.setUpdatedAt(LocalDateTime.now());
            return configurationRepository.save(configuration);
        }
    }

    /**
     * Supprime une configuration
     */
    public void deleteConfiguration(Long id) {
        configurationRepository.deleteById(id);
    }
}