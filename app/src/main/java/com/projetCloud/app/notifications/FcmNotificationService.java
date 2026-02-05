package com.projetCloud.app.notifications;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

/**
 * Service pour envoyer les notifications push via Firebase Cloud Messaging (FCM)
 */
@Service
public class FcmNotificationService {

    /**
     * Envoie une notification push à un utilisateur via son FCM token
     *
     * @param fcmToken Le token FCM de l'utilisateur
     * @param title Titre de la notification
     * @param body Contenu de la notification
     * @param data Données additionnelles (optionnel)
     * @return L'ID du message envoyé
     */
    public String sendNotification(String fcmToken, String title, String body, Map<String, String> data) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            System.err.println("[FCM] ❌ Token FCM vide, notification non envoyée");
            return null;
        }

        try {
            System.out.println("[FCM] Envoi notification - Title: " + title + ", Body: " + body);
            
            Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

            Message.Builder messageBuilder = Message.builder()
                .setToken(fcmToken)
                .setNotification(notification);

            // Ajouter les données si fournies
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
                System.out.println("[FCM] Données ajoutées: " + data.size() + " items");
            }

            Message message = messageBuilder.build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            System.out.println("[FCM] ✅ Notification envoyée avec succès - messageId: " + messageId);
            return messageId;

        } catch (Exception e) {
            System.err.println("[FCM] ❌ Erreur envoi notification: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Construit une notification de changement de status
     *
     * @param reportType Type du signalement (ex: "Trou")
     * @param oldStatus Ancien status
     * @param newStatus Nouveau status
     * @return Map avec title et body
     */
    public Map<String, String> buildStatusChangeNotification(String reportType, String oldStatus, String newStatus) {
        Map<String, String> notification = new HashMap<>();

        // Emoji basé sur le nouveau status
        String emoji = getStatusEmoji(newStatus);

        notification.put("title", emoji + " Signalement mis à jour");
        notification.put("body", String.format("Votre signalement (%s) est maintenant %s", reportType, newStatus));

        return notification;
    }

    /**
     * Retourne l'emoji approprié pour le status
     */
    private String getStatusEmoji(String status) {
        String normalizedStatus = (status != null ? status.toLowerCase() : "").replace("é", "e").replace(" ", "_");

        return switch (normalizedStatus) {
            case "nouveau" -> "✨"; // Nouveau
            case "en_cours" -> "🔧"; // En cours
            case "termine" -> "✅"; // Terminé
            case "annule" -> "❌"; // Annulé
            default -> "📢";
        };
    }

    /**
     * Envoie une notification de changement de status
     *
     * @param fcmToken Token FCM de l'utilisateur
     * @param reportType Type du signalement
     * @param oldStatus Ancien status
     * @param newStatus Nouveau status
     */
    public void notifyStatusChange(String fcmToken, String reportType, String oldStatus, String newStatus) {
        System.out.println("[FCM StatusChange] Début - Type: " + reportType + ", Old: " + oldStatus + ", New: " + newStatus);
        
        if (oldStatus != null && oldStatus.equalsIgnoreCase(newStatus)) {
            System.out.println("[FCM StatusChange] Status inchangé, notification non envoyée");
            return;
        }

        Map<String, String> notificationData = buildStatusChangeNotification(reportType, oldStatus, newStatus);

        Map<String, String> data = new HashMap<>();
        data.put("reportType", reportType != null ? reportType : "Signalement");
        data.put("oldStatus", oldStatus != null ? oldStatus : "Inconnu");
        data.put("newStatus", newStatus != null ? newStatus : "Inconnu");

        System.out.println("[FCM StatusChange] Title: " + notificationData.get("title"));
        System.out.println("[FCM StatusChange] Body: " + notificationData.get("body"));
        
        sendNotification(
            fcmToken,
            notificationData.get("title"),
            notificationData.get("body"),
            data
        );
    }
}
