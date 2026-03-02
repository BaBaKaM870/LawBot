package dto;

/**
 * Données envoyées par le joueur quand il pose une question à un témoin.
 *
 * Exemple de body JSON :
 * {
 *   "trialId": 1,
 *   "witnessId": 3,
 *   "question": "Où étiez-vous le soir du 12 mars ?"
 * }
 */
public record QuestionDTO(
        Long trialId,
        Long witnessId,
        String question
) {}
