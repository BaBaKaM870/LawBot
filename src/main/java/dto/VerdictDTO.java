package dto;

/**
 * Résultat final envoyé au frontend quand le jury délibère.
 *
 * Exemple de réponse JSON :
 * {
 *   "status": "GUILTY",
 *   "explanation": "Les preuves présentées et les contradictions détectées ont convaincu le jury.",
 *   "playerScore": 78,
 *   "guiltyVotes": 9,
 *   "totalJurors": 12,
 *   "suspectName": "Paul Martin",
 *   "wasActuallyGuilty": true
 * }
 */
public record VerdictDTO(
        String status,               // "GUILTY" ou "NOT_GUILTY"
        String explanation,          // Narration du verdict pour le joueur
        int playerScore,             // Score final du joueur (0 à 100)
        int guiltyVotes,             // Nombre de jurés ayant voté coupable
        int totalJurors,             // Taille totale du jury
        String suspectName,          // Nom du suspect
        boolean wasActuallyGuilty    // Révèle la vérité après le verdict
) {}
