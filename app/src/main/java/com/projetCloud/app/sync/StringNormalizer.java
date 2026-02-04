package com.projetCloud.app.sync;

/**
 * Utilitaire pour normaliser les strings pour la comparaison
 * Gère les accents, tirets, underscores, etc.
 */
public class StringNormalizer {

    /**
     * Normalise une string pour la comparaison
     * - Supprime les accents (é -> e, à -> a, etc)
     * - Trim + lowercase
     * - Remplace underscores/tirets par espaces
     * - Supprime espaces multiples
     *
     * Exemples:
     * - "Créé" -> "cree"
     * - "En cours" -> "en cours"
     * - "en_cours" -> "en cours"
     * - "En-Cours" -> "en cours"
     * - "TERMINÉ" -> "termine"
     */
    public static String normalize(String str) {
        if (str == null) return "";
        
        // Supprimer les accents et caractères spéciaux
        // Décompose les caractères accentués (é = e + ´) puis garde juste e
        String normalized = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        
        // Trim + lowercase
        normalized = normalized.trim().toLowerCase();
        
        // Remplacer underscores/tirets par espaces
        normalized = normalized.replace("_", " ").replace("-", " ");
        
        // Supprimer les espaces multiples
        normalized = normalized.replaceAll("\\s+", " ");
        
        return normalized;
    }

    /**
     * Compare deux strings après normalisation
     */
    public static boolean equalsNormalized(String str1, String str2) {
        return normalize(str1).equals(normalize(str2));
    }

    /**
     * Teste la normalisation
     */
    public static void main(String[] args) {
        System.out.println("\"Créé\" -> \"" + normalize("Créé") + "\"");
        System.out.println("\"En cours\" -> \"" + normalize("En cours") + "\"");
        System.out.println("\"en_cours\" -> \"" + normalize("en_cours") + "\"");
        System.out.println("\"En-Cours\" -> \"" + normalize("En-Cours") + "\"");
        System.out.println("\"TERMINÉ\" -> \"" + normalize("TERMINÉ") + "\"");
        System.out.println("\"À  Tester\" -> \"" + normalize("À  Tester") + "\"");
        
        System.out.println("\nComparaisons:");
        System.out.println("Créé == cree: " + equalsNormalized("Créé", "cree"));
        System.out.println("En cours == en_cours: " + equalsNormalized("En cours", "en_cours"));
    }
}
