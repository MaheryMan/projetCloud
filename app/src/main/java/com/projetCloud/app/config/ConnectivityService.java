package com.projetCloud.app.config;

import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class ConnectivityService {

    @Autowired
    private Firestore firestore;

    /**
     * Vérifie la connectivité à Firebase en essayant de lire un document test.
     * Retourne true si connecté, false sinon.
     */
    public boolean isFirebaseOnline() {
        try {
            // Essayer de lire un document fictif pour tester la connexion
            firestore.collection("test").document("ping").get().get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            // En cas d'erreur (pas de connexion, etc.), considérer comme offline
            return false;
        }
    }
}