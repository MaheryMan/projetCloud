package com.projetCloud.app.config;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import io.grpc.netty.shaded.io.netty.handler.timeout.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class ConnectivityService {

    @Autowired
    private Firestore firestore;

    /**
     * Vérifie la connectivité à Firebase en essayant de lire un document test.
     * Retourne true si connecté, false sinon.
     * @throws java.util.concurrent.TimeoutException 
     */
    public boolean isFirebaseOnline() throws java.util.concurrent.TimeoutException {
        try {
            ApiFuture<DocumentSnapshot> future = firestore.collection("test").document("ping").get();

            // Timeout 3 secondes
            future.get(3, TimeUnit.SECONDS);

            return true;
        } catch (TimeoutException e) {
            // Firebase injoignable
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            // Ici Firebase est joignable MAIS erreur logique possible
            // ex: permission denied, document absent
            return true;
        }
    }
}