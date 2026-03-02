package service;

import dto.QuestionDTO;
import dto.WitnessResponseDTO;
import model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WitnessService {

    // Seuil de fiabilité à partir duquel un témoin peut se contredire sous pression
    private static final double CONTRADICTION_THRESHOLD = 0.6;

    /**
     * Interroge un témoin avec une question du joueur (via l'API IA)
     * Retourne la réponse du témoin et détecte d'éventuelles contradictions
     */
    public WitnessResponseDTO interrogate(Witness witness, QuestionDTO question, Case currentCase) {
        WitnessResponseDTO response = new WitnessResponseDTO();
        response.setWitnessName(witness.getName());
        response.setQuestion(question.getContent());

        // Génération de la réponse selon la fiabilité du témoin
        String answer = generateAnswer(witness, question, currentCase);
        response.setAnswer(answer);

        // Détection de contradiction avec les déclarations précédentes
        Optional<Contradiction> contradiction = detectContradiction(witness, answer);
        contradiction.ifPresent(c -> {
            response.setContradictionDetected(true);
            response.setContradiction(c);
            witness.addContradiction(c); // Polymorphisme : Witness hérite de Person
        });

        // Mise à jour du niveau de stress du témoin
        witness.increaseStress(question.isAggressive() ? 0.2 : 0.05);
        response.setWitnessStressLevel(witness.getStressLevel());

        // Enregistrement de la réponse dans l'historique du témoin
        Statement stmt = new Statement();
        stmt.setContent(answer);
        stmt.setWitnessName(witness.getName());
        stmt.setQuestionAsked(question.getContent());
        witness.addStatement(stmt);

        return response;
    }

    /**
     * Confronte deux témoins sur une même question (détecte les incohérences)
     */
    public List<Contradiction> confrontWitnesses(Witness w1, Witness w2, String topic) {
        List<Contradiction> contradictions = new ArrayList<>();

        List<Statement> stmts1 = w1.getStatements();
        List<Statement> stmts2 = w2.getStatements();

        for (Statement s1 : stmts1) {
            for (Statement s2 : stmts2) {
                if (isContradictory(s1, s2, topic)) {
                    Contradiction c = new Contradiction();
                    c.setId(UUID.randomUUID().toString());
                    c.setStatement1(s1);
                    c.setStatement2(s2);
                    c.setTopic(topic);
                    c.setDescription(String.format(
                        "Contradiction entre %s et %s sur le sujet : %s",
                        w1.getName(), w2.getName(), topic
                    ));
                    c.setSeverity(calculateSeverity(w1.getReliability(), w2.getReliability()));
                    contradictions.add(c);
                }
            }
        }
        return contradictions;
    }

    /**
     * Calcule le score global de crédibilité d'un témoin
     */
    public double computeCredibilityScore(Witness witness) {
        double base = witness.getReliability();
        double stressPenalty = witness.getStressLevel() * 0.2;
        double contradictionPenalty = witness.getContradictions().size() * 0.15;
        return Math.max(0.0, Math.min(1.0, base - stressPenalty - contradictionPenalty));
    }

    /**
     * Récupère tous les témoins d'une affaire triés par crédibilité
     */
    public List<Witness> getWitnessesByCredibility(Case currentCase) {
        List<Witness> witnesses = new ArrayList<>(currentCase.getWitnesses());
        witnesses.sort((w1, w2) -> Double.compare(
            computeCredibilityScore(w2),
            computeCredibilityScore(w1)
        ));
        return witnesses;
    }

    // --- Méthodes privées ---

    private String generateAnswer(Witness witness, QuestionDTO question, Case currentCase) {
        // Si le témoin est sous stress élevé et peu fiable → réponse incohérente
        if (witness.getStressLevel() > 0.7 && witness.getReliability() < CONTRADICTION_THRESHOLD) {
            return generateInconsistentAnswer(witness, question);
        }
        // Sinon réponse cohérente basée sur sa déclaration initiale
        return elaborateOnStatement(witness.getInitialStatement(), question.getContent());
    }

    private String generateInconsistentAnswer(Witness witness, QuestionDTO question) {
        // Retourne une réponse qui diffère légèrement de la déclaration initiale
        String base = witness.getInitialStatement() != null
            ? witness.getInitialStatement().getContent()
            : "Je ne me souviens plus très bien...";
        return "En fait... " + base + " Enfin, je crois. Je suis un peu confus.";
    }

    private String elaborateOnStatement(Statement initialStatement, String question) {
        if (initialStatement == null) return "Je n'ai rien à ajouter.";
        return "Pour être précis : " + initialStatement.getContent() +
               ". C'est tout ce que j'ai observé concernant votre question.";
    }

    private Optional<Contradiction> detectContradiction(Witness witness, String newAnswer) {
        for (Statement previous : witness.getStatements()) {
            if (isContradictory(previous.getContent(), newAnswer)) {
                Contradiction c = new Contradiction();
                c.setId(UUID.randomUUID().toString());
                c.setStatement1(previous);
                Statement newStmt = new Statement();
                newStmt.setContent(newAnswer);
                newStmt.setWitnessName(witness.getName());
                c.setStatement2(newStmt);
                c.setDescription("Le témoin contredit une déclaration précédente.");
                c.setSeverity(0.7);
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    private boolean isContradictory(Statement s1, Statement s2, String topic) {
        if (s1 == null || s2 == null) return false;
        return isContradictory(s1.getContent(), s2.getContent());
    }

    private boolean isContradictory(String text1, String text2) {
        if (text1 == null || text2 == null) return false;
        // Logique simplifiée : contradiction si les textes sont très différents
        // En production, on déléguerait à un service NLP ou à l'IA
        String[] negations = {"ne pas", "jamais", "non", "aucun"};
        for (String neg : negations) {
            if (text1.contains(neg) != text2.contains(neg)) return true;
        }
        return false;
    }

    private double calculateSeverity(double reliability1, double reliability2) {
        // Plus les deux témoins sont fiables, plus la contradiction est sévère
        return (reliability1 + reliability2) / 2.0;
    }
}
