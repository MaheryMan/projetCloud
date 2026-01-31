package com.projetCloud.app.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}