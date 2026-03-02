package service;

import dto.QuestionDTO;
import dto.WitnessResponseDTO;
import model.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WitnessService {

    private static final double CONTRADICTION_THRESHOLD = 0.6;

    // Registre en mémoire des témoins par ID — alimenté par TrialService au démarrage d'un procès
    private final Map<Long, Witness> witnessRegistry = new HashMap<>();

    private final TrialService trialService;

    // @Lazy évite la dépendance circulaire WitnessService ↔ TrialService
    public WitnessService(@Lazy TrialService trialService) {
        this.trialService = trialService;
    }

    /** Enregistre les témoins d'une affaire pour pouvoir les retrouver par ID. */
    public void registerWitnesses(Case cas) {
        if (cas.getWitnesses() != null) {
            for (Witness w : cas.getWitnesses()) {
                if (w.getId() != null) {
                    witnessRegistry.put(w.getId(), w);
                }
            }
        }
    }

    /**
     * Point d'entrée appelé par WitnessController.
     * Utilise le trialId contenu dans le QuestionDTO pour retrouver le contexte.
     */
    public WitnessResponseDTO processQuestion(Long witnessId, QuestionDTO question) {
        Trial trial = trialService.getTrial(question.trialId());
        Witness witness = findWitnessInTrial(trial, witnessId);
        return interrogate(witness, question, trial.getLawCase());
    }

    /**
     * Confronte un témoin avec une preuve et retourne la contradiction trouvée, ou null.
     */
    public Contradiction checkContradiction(Long trialId, Long witnessId, Long evidenceId) {
        Trial trial = trialService.getTrial(trialId);
        Witness witness = findWitnessInTrial(trial, witnessId);
        Evidence evidence = findEvidenceInCase(trial.getLawCase(), evidenceId);

        for (Statement stmt : witness.getStatements()) {
            if (isContradictory(stmt.getContent(), evidence.getDescription())) {
                Contradiction c = new Contradiction();
                c.setStatement1(stmt);
                c.setDescription(String.format(
                    "Le témoin %s contredit la preuve \"%s\".",
                    witness.getName(), evidence.getDescription()
                ));
                c.setSeverity(witness.getReliability());
                c.setDetected(true);
                witness.addContradiction(c);
                return c;
            }
        }
        return null;
    }

    /**
     * Interroge un témoin — appelé par TrialService.
     */
    public WitnessResponseDTO interrogate(Witness witness, QuestionDTO question, Case currentCase) {
        String answer = generateAnswer(witness, question.question());

        boolean contradictionDetected = false;
        String contradictionDescription = null;

        Optional<Contradiction> contradiction = detectContradiction(witness, answer);
        if (contradiction.isPresent()) {
            contradictionDetected = true;
            contradictionDescription = contradiction.get().getDescription();
            witness.addContradiction(contradiction.get());
            witness.setCredibility(Math.max(0, witness.getCredibility() - 15));
        }

        witness.increaseStress(0.05);

        Statement stmt = new Statement();
        stmt.setContent(answer);
        stmt.setWitness(witness);
        stmt.setWitnessName(witness.getName());
        stmt.setQuestionAsked(question.question());
        witness.addStatement(stmt);

        return new WitnessResponseDTO(
            witness.getName(),
            question.question(),
            answer,
            witness.getCredibility(),
            contradictionDetected,
            contradictionDescription
        );
    }

    /**
     * Confronte deux témoins et retourne les contradictions entre leurs déclarations.
     */
    public List<Contradiction> confrontWitnesses(Witness w1, Witness w2, String topic) {
        List<Contradiction> contradictions = new ArrayList<>();

        List<Statement> stmts1 = w1.getStatements();
        List<Statement> stmts2 = w2.getStatements();

        if (stmts1 == null || stmts2 == null) return contradictions;

        for (Statement s1 : stmts1) {
            for (Statement s2 : stmts2) {
                if (isContradictory(s1.getContent(), s2.getContent())) {
                    Contradiction c = new Contradiction();
                    c.setStatement1(s1);
                    c.setStatement2(s2);
                    c.setTopic(topic);
                    c.setDescription(String.format(
                        "Contradiction entre %s et %s sur le sujet : %s",
                        w1.getName(), w2.getName(), topic
                    ));
                    c.setSeverity((w1.getReliability() + w2.getReliability()) / 2.0);
                    contradictions.add(c);
                }
            }
        }
        return contradictions;
    }

    /** Calcule le score de crédibilité d'un témoin (0.0 à 1.0). */
    public double computeCredibilityScore(Witness witness) {
        double base = witness.getReliability();
        double stressPenalty = witness.getStressLevel() * 0.2;
        double contradictionPenalty = witness.getContradictions().size() * 0.15;
        return Math.max(0.0, Math.min(1.0, base - stressPenalty - contradictionPenalty));
    }

    /** Retourne les témoins d'une affaire triés par crédibilité décroissante. */
    public List<Witness> getWitnessesByCredibility(Case currentCase) {
        List<Witness> witnesses = new ArrayList<>(currentCase.getWitnesses());
        witnesses.sort((w1, w2) -> Double.compare(
            computeCredibilityScore(w2),
            computeCredibilityScore(w1)
        ));
        return witnesses;
    }

    // --- Méthodes privées ---

    private String generateAnswer(Witness witness, String question) {
        if (witness.getStressLevel() > 0.7 && witness.getReliability() < CONTRADICTION_THRESHOLD) {
            return generateInconsistentAnswer(witness);
        }
        return elaborateOnStatement(witness.getInitialStatement(), question);
    }

    private String generateInconsistentAnswer(Witness witness) {
        Statement initial = witness.getInitialStatement();
        String base = (initial != null) ? initial.getContent() : "Je ne me souviens plus très bien...";
        return "En fait... " + base + " Enfin, je crois. Je suis un peu confus.";
    }

    private String elaborateOnStatement(Statement initialStatement, String question) {
        if (initialStatement == null) return "Je n'ai rien à ajouter.";
        return "Pour être précis : " + initialStatement.getContent()
            + ". C'est tout ce que j'ai observé concernant votre question.";
    }

    private Optional<Contradiction> detectContradiction(Witness witness, String newAnswer) {
        for (Statement previous : witness.getStatements()) {
            if (isContradictory(previous.getContent(), newAnswer)) {
                Statement newStmt = new Statement();
                newStmt.setContent(newAnswer);
                newStmt.setWitness(witness);
                Contradiction c = new Contradiction();
                c.setStatement1(previous);
                c.setStatement2(newStmt);
                c.setDescription("Le témoin contredit une déclaration précédente.");
                c.setSeverity(witness.getReliability());
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    private boolean isContradictory(String text1, String text2) {
        if (text1 == null || text2 == null) return false;
        String[] negations = {"ne pas", "jamais", "non", "aucun"};
        for (String neg : negations) {
            if (text1.contains(neg) != text2.contains(neg)) return true;
        }
        return false;
    }

    private Witness findWitnessInTrial(Trial trial, Long witnessId) {
        return trial.getLawCase().getWitnesses().stream()
            .filter(w -> witnessId.equals(w.getId()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Témoin introuvable : " + witnessId));
    }

    private Evidence findEvidenceInCase(Case cas, Long evidenceId) {
        return cas.getEvidences().stream()
            .filter(e -> evidenceId.equals(e.getId()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Preuve introuvable : " + evidenceId));
    }
}
