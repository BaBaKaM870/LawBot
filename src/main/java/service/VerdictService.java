package service;

import dto.VerdictDTO;
import model.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VerdictService {

    // Seuil de conviction pour un verdict de culpabilité
    private static final double GUILT_THRESHOLD = 0.65;
    // Seuil minimum pour un acquittement franc
    private static final double ACQUITTAL_THRESHOLD = 0.35;

    /**
     * Calcule le verdict final à partir de l'état du procès
     */
    public VerdictDTO calculateVerdict(Trial trial) {
        Case currentCase = trial.getCurrentCase();

        // 1. Score des preuves
        double evidenceScore = computeEvidenceScore(currentCase.getEvidences());

        // 2. Score des témoins (crédibilité × impact)
        double witnessScore = computeWitnessScore(currentCase.getWitnesses());

        // 3. Score du jury (conviction moyenne)
        double juryScore = computeJuryScore(currentCase.getJury());

        // 4. Bonus/malus selon la performance de l'avocat (joueur)
        double lawyerBonus = computeLawyerBonus(trial);

        // Score global pondéré
        double finalScore = (evidenceScore * 0.40)
                          + (witnessScore * 0.30)
                          + (juryScore   * 0.20)
                          + (lawyerBonus * 0.10);

        finalScore = Math.max(0.0, Math.min(1.0, finalScore));

        return buildVerdict(finalScore, evidenceScore, witnessScore, juryScore, currentCase);
    }

    /**
     * Met à jour le niveau de conviction du jury selon un événement
     * (preuve présentée, contradiction découverte, etc.)
     */
    public void updateJuryConviction(List<JuryMember> jury, double impact, boolean favorableToDefense) {
        for (JuryMember member : jury) {
            double delta = impact * (favorableToDefense ? -1 : 1) * (0.8 + Math.random() * 0.4);
            member.setConvictionLevel(
                Math.max(0.0, Math.min(1.0, member.getConvictionLevel() + delta))
            );
        }
    }

    /**
     * Retourne le niveau de conviction moyen du jury (utile pour l'UI)
     */
    public double getAverageJuryConviction(List<JuryMember> jury) {
        return jury.stream()
            .mapToDouble(JuryMember::getConvictionLevel)
            .average()
            .orElse(0.5);
    }

    // --- Calculs intermédiaires ---

    private double computeEvidenceScore(List<Evidence> evidences) {
        if (evidences == null || evidences.isEmpty()) return 0.5;

        double total = 0.0;
        double weightSum = 0.0;

        for (Evidence e : evidences) {
            double effectiveWeight = e.getWeight();
            // Une preuve contestée perd 50% de son poids
            if (e.isContested()) effectiveWeight *= 0.5;
            // Une preuve non authentique ne compte pas contre la défense
            double contribution = e.isAuthentic() ? effectiveWeight : -effectiveWeight * 0.3;
            total += contribution;
            weightSum += e.getWeight();
        }

        return weightSum > 0 ? Math.max(0.0, Math.min(1.0, total / weightSum)) : 0.5;
    }

    private double computeWitnessScore(List<Witness> witnesses) {
        if (witnesses == null || witnesses.isEmpty()) return 0.5;

        double total = 0.0;
        for (Witness w : witnesses) {
            double credibility = w.getReliability();
            // Pénalité pour chaque contradiction détectée
            double penalty = w.getContradictions().size() * 0.1;
            total += Math.max(0.0, credibility - penalty);
        }
        return Math.min(1.0, total / witnesses.size());
    }

    private double computeJuryScore(List<JuryMember> jury) {
        return jury.stream()
            .mapToDouble(JuryMember::getConvictionLevel)
            .average()
            .orElse(0.5);
    }

    private double computeLawyerBonus(Trial trial) {
        // Basé sur les actions réussies du joueur : contradictions révélées, preuves contestées
        int successfulActions = trial.getSuccessfulActionsCount();
        int totalActions = trial.getTotalActionsCount();
        if (totalActions == 0) return 0.0;
        // Un joueur performant (>70% actions réussies) reçoit un bonus négatif (favorable à la défense)
        double ratio = (double) successfulActions / totalActions;
        return ratio > 0.7 ? -(ratio * 0.1) : 0.0;
    }

    private VerdictDTO buildVerdict(double score, double evidenceScore,
                                    double witnessScore, double juryScore, Case currentCase) {
        VerdictDTO verdict = new VerdictDTO();
        verdict.setConvictionScore(score);
        verdict.setCaseId(currentCase.getId());

        // Détermination du verdict
        if (score >= GUILT_THRESHOLD) {
            verdict.setVerdict("COUPABLE");
            verdict.setOutcome("Votre client est reconnu coupable.");
            verdict.setSentence(determineSentence(score, currentCase.getCrimeType()));
        } else if (score <= ACQUITTAL_THRESHOLD) {
            verdict.setVerdict("NON COUPABLE");
            verdict.setOutcome("Félicitations ! Votre client est acquitté.");
            verdict.setSentence("Remise en liberté immédiate.");
        } else {
            verdict.setVerdict("DOUTE RAISONNABLE");
            verdict.setOutcome("Le jury ne parvient pas à un verdict unanime. Nouveau procès possible.");
            verdict.setSentence("En attente de décision.");
        }

        // Explication détaillée
        verdict.setExplanation(buildExplanation(score, evidenceScore, witnessScore, juryScore));

        return verdict;
    }

    private String determineSentence(double score, String crimeType) {
        int years = (int) (score * 20); // Max 20 ans selon le score
        return switch (crimeType.toLowerCase()) {
            case "meurtre" -> years + " ans de réclusion criminelle.";
            case "fraude"  -> years / 2 + " ans d'emprisonnement et amende.";
            case "vol"     -> years / 3 + " ans d'emprisonnement.";
            default        -> years / 4 + " ans d'emprisonnement.";
        };
    }

    private String buildExplanation(double finalScore, double evidenceScore,
                                    double witnessScore, double juryScore) {
        return String.format(
            "Score final : %.0f%%. " +
            "Preuves à charge : %.0f%%. " +
            "Crédibilité des témoins : %.0f%%. " +
            "Conviction du jury : %.0f%%.",
            finalScore * 100, evidenceScore * 100,
            witnessScore * 100, juryScore * 100
        );
    }
}
