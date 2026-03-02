package dto;

/**
 * Réponse renvoyée au frontend après qu'un témoin a répondu à une question.
 *
 * Exemple de réponse JSON :
 * {
 *   "witnessName": "Marie Dupont",
 *   "question": "Où étiez-vous le soir du 12 mars ?",
 *   "response": "J'étais chez moi, je vous l'assure.",
 *   "credibility": 42,
 *   "contradictionDetected": true,
 *   "contradictionDescription": "Le témoin a dit être chez lui, mais avait déclaré plus tôt être au bar."
 * }
 */
public record WitnessResponseDTO(
        String witnessName,
        String question,
        String response,
        int credibility,
        boolean contradictionDetected,
        String contradictionDescription  // null si pas de contradiction détectée
) {}
