package dto;

import java.util.List;

/**
 * Résultat final envoyé au frontend quand le jury délibère.
 */
public record VerdictDTO(
        String status,               // "GUILTY" ou "NOT_GUILTY"
        String explanation,          // Narration du verdict pour le joueur
        int playerScore,             // Score final du joueur (0 à 100)
        int guiltyVotes,             // Nombre de jurés ayant voté coupable
        int totalJurors,             // Taille totale du jury
        String suspectName,          // Nom du suspect
        boolean wasActuallyGuilty,   // Révèle la vérité après le verdict
        String grade,                // Mention : S / A / B / C / D / F
        List<String> feedback        // Points de feedback personnalisés
) {}
