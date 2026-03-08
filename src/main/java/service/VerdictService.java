package service;

import dto.VerdictDTO;
import model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class VerdictService {

    private static final double GUILT_THRESHOLD = 0.65;
    private static final double ACQUITTAL_THRESHOLD = 0.35;
    private static final Random random = new Random();

    private static final String[] JURY_COMMENTS = {
        "Le juré n°3 a failli s'endormir pendant les plaidoiries.",
        "Le jury a délibéré 12 minutes, dont 8 pour commander des sandwichs.",
        "Le juré n°5 aurait voté en lançant une pièce selon une source anonyme.",
        "Le jury était partagé jusqu'à ce que quelqu'un crie « On rentre dîner ! ».",
        "Le juré n°1 a renversé son café au moment crucial — coïncidence ?",
        "D'après des sources, le juré n°6 pensait assister à un concours de pâtisserie.",
        "Le juré n°2 a demandé à trois reprises si l'accusé était 'celui de la télé'.",
        "Le juré n°4 a voté en regardant ostensiblement ses chaussures.",
    };

    /**
     * Calcule le verdict final à partir de l'état du procès.
     */
    public VerdictDTO calculateVerdict(Trial trial) {
        Case currentCase = trial.getCurrentCase();

        double evidenceScore = computeEvidenceScore(currentCase.getEvidences());
        double witnessScore  = computeWitnessScore(currentCase.getWitnesses());
        double juryScore     = computeJuryScore(currentCase.getJury());
        double lawyerBonus   = computeLawyerBonus(trial);

        double finalScore = (evidenceScore * 0.40)
                          + (witnessScore  * 0.30)
                          + (juryScore     * 0.20)
                          + (lawyerBonus   * 0.10);

        finalScore = Math.max(0.0, Math.min(1.0, finalScore));

        return buildVerdict(finalScore, evidenceScore, witnessScore, juryScore, trial);
    }

    /**
     * Met à jour le niveau de conviction du jury selon un événement.
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
     * Retourne le niveau de conviction moyen du jury.
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
            if (e.isContested()) effectiveWeight *= 0.5;
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
            double penalty = w.getContradictions().size() * 0.1;
            total += Math.max(0.0, w.getReliability() - penalty);
        }
        return Math.min(1.0, total / witnesses.size());
    }

    private double computeJuryScore(List<JuryMember> jury) {
        if (jury == null || jury.isEmpty()) return 0.5;
        return jury.stream()
            .mapToDouble(JuryMember::getConvictionLevel)
            .average()
            .orElse(0.5);
    }

    private double computeLawyerBonus(Trial trial) {
        int totalActions = trial.getTotalActionsCount();
        if (totalActions == 0) return 0.0;
        double ratio = (double) trial.getSuccessfulActionsCount() / totalActions;
        // Un joueur performant (>70% actions réussies) reçoit un bonus favorable à la défense
        return ratio > 0.7 ? -(ratio * 0.1) : 0.0;
    }

    private VerdictDTO buildVerdict(double score, double evidenceScore,
                                    double witnessScore, double juryScore, Trial trial) {
        Case currentCase = trial.getCurrentCase();
        List<JuryMember> jury = currentCase.getJury();

        String status;
        if (score >= GUILT_THRESHOLD) {
            status = "GUILTY";
        } else {
            status = "NOT_GUILTY";
        }

        String explanation = buildExplanation(score, evidenceScore, witnessScore, juryScore);

        int playerScore = (int) Math.round((1.0 - score) * 100); // Score inversé : défense veut score bas

        int guiltyVotes = (jury == null) ? 0 : (int) jury.stream()
            .filter(m -> m.getConvictionLevel() > 0.5)
            .count();

        int totalJurors = (jury == null) ? 0 : jury.size();

        String suspectName = currentCase.getSuspect() != null
            ? currentCase.getSuspect().getName()
            : "Inconnu";

        boolean wasActuallyGuilty = currentCase.getSuspect() != null
            && currentCase.getSuspect().isGuilty();

        String grade    = computeGrade(playerScore);
        List<String> feedback = buildFeedback(score, evidenceScore, witnessScore, juryScore, trial);

        return new VerdictDTO(status, explanation, playerScore, guiltyVotes, totalJurors,
                              suspectName, wasActuallyGuilty, grade, feedback);
    }

    private String computeGrade(int playerScore) {
        if (playerScore >= 80) return "S";
        if (playerScore >= 65) return "A";
        if (playerScore >= 50) return "B";
        if (playerScore >= 35) return "C";
        if (playerScore >= 20) return "D";
        return "F";
    }

    private List<String> buildFeedback(double finalScore, double evidenceScore,
                                       double witnessScore, double juryScore, Trial trial) {
        List<String> points = new ArrayList<>();

        if (evidenceScore > 0.65) {
            points.add("⚠ Les preuves à charge étaient très solides — contestez plus de preuves douteuses.");
        } else {
            points.add("✔ Bonne gestion des preuves : vous avez bien neutralisé l'accusation.");
        }

        if (witnessScore > 0.65) {
            points.add("⚠ Les témoins sont restés trop crédibles — posez des questions plus déstabilisantes.");
        } else {
            points.add("✔ Vous avez sérieusement entamé la crédibilité des témoins.");
        }

        if (juryScore > 0.65) {
            points.add("⚠ Le jury n'a pas été convaincu — ciblez mieux les preuves et les contradictions.");
        } else if (juryScore < 0.35) {
            points.add("✔ Le jury a clairement pris votre parti !");
        }

        int total = trial.getTotalActionsCount();
        if (total > 0) {
            int ratio = (int) ((double) trial.getSuccessfulActionsCount() / total * 100);
            if (ratio >= 60) {
                points.add("✔ Précision exemplaire : " + ratio + "% d'actions réussies.");
            } else if (ratio < 30) {
                points.add("⚠ Seulement " + ratio + "% d'actions réussies — analysez mieux avant d'agir.");
            }
        }

        if (points.isEmpty()) {
            points.add("💡 Procès serré. Cherchez les preuves douteuses et les témoins peu fiables.");
        }

        return points;
    }

    private String determineSentence(double score, Case.CrimeType crimeType) {
        int years = (int) (score * 20);
        return switch (crimeType) {
            case MURDER     -> years + " ans de réclusion criminelle.";
            case FRAUD      -> years / 2 + " ans d'emprisonnement et amende.";
            case THEFT      -> years / 3 + " ans d'emprisonnement.";
            default         -> years / 4 + " ans d'emprisonnement.";
        };
    }

    private String buildExplanation(double finalScore, double evidenceScore,
                                    double witnessScore, double juryScore) {
        String juryComment = JURY_COMMENTS[random.nextInt(JURY_COMMENTS.length)];
        return String.format(
            "Score final : %.0f%% | Preuves : %.0f%% | Témoins : %.0f%% | Jury : %.0f%% — %s",
            finalScore * 100, evidenceScore * 100, witnessScore * 100, juryScore * 100,
            juryComment
        );
    }
}
