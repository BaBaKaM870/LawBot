package service;

import dto.QuestionDTO;
import dto.VerdictDTO;
import dto.WitnessResponseDTO;
import model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TrialService {

    private final CaseGeneratorService caseGeneratorService;
    private final WitnessService witnessService;
    private final VerdictService verdictService;

    // Stockage en mémoire des procès actifs (sessionId → Trial)
    private final Map<String, Trial> activeTrials = new HashMap<>();

    public TrialService(CaseGeneratorService caseGeneratorService,
                        WitnessService witnessService,
                        VerdictService verdictService) {
        this.caseGeneratorService = caseGeneratorService;
        this.witnessService = witnessService;
        this.verdictService = verdictService;
    }

    // ==============================
    //  GESTION DU PROCÈS
    // ==============================

    /**
     * Démarre un nouveau procès avec une affaire générée aléatoirement
     */
    public Trial startTrial(String sessionId) {
        Case newCase = caseGeneratorService.generateRandomCase();
        return startTrial(sessionId, newCase);
    }

    /**
     * Démarre un procès avec un type de crime précis
     */
    public Trial startTrial(String sessionId, String crimeType) {
        Case newCase = caseGeneratorService.generateCaseByType(crimeType);
        return startTrial(sessionId, newCase);
    }

    private Trial startTrial(String sessionId, Case cas) {
        Trial trial = new Trial();
        trial.setId(UUID.randomUUID().toString());
        trial.setSessionId(sessionId);
        trial.setCurrentCase(cas);
        trial.setCurrentPhase(TrialPhase.OPENING_STATEMENTS);
        trial.setActive(true);
        trial.setSuccessfulActionsCount(0);
        trial.setTotalActionsCount(0);
        trial.setStartTime(new Date());

        activeTrials.put(sessionId, trial);
        return trial;
    }

    /**
     * Récupère un procès actif
     */
    public Trial getTrial(String sessionId) {
        Trial trial = activeTrials.get(sessionId);
        if (trial == null) throw new RuntimeException("Aucun procès actif pour la session : " + sessionId);
        return trial;
    }

    /**
     * Passe à la phase suivante du procès
     */
    public Trial advancePhase(String sessionId) {
        Trial trial = getTrial(sessionId);
        TrialPhase next = getNextPhase(trial.getCurrentPhase());
        trial.setCurrentPhase(next);

        // Clôture automatique si on atteint la phase de verdict
        if (next == TrialPhase.VERDICT) {
            trial.setActive(false);
        }
        return trial;
    }

    /**
     * Retourne la phase suivante dans l'ordre du procès
     */
    public TrialPhase getNextPhase(TrialPhase current) {
        return switch (current) {
            case OPENING_STATEMENTS  -> TrialPhase.PROSECUTION_CASE;
            case PROSECUTION_CASE    -> TrialPhase.DEFENSE_CASE;
            case DEFENSE_CASE        -> TrialPhase.CROSS_EXAMINATION;
            case CROSS_EXAMINATION   -> TrialPhase.CLOSING_ARGUMENTS;
            case CLOSING_ARGUMENTS   -> TrialPhase.VERDICT;
            case VERDICT             -> TrialPhase.VERDICT; // Fin du procès
        };
    }

    // ==============================
    //  ACTIONS PENDANT LE PROCÈS
    // ==============================

    /**
     * Le joueur (avocat) interroge un témoin
     */
    public WitnessResponseDTO questionWitness(String sessionId, String witnessId, QuestionDTO question) {
        Trial trial = getTrial(sessionId);
        validatePhase(trial, TrialPhase.DEFENSE_CASE, TrialPhase.CROSS_EXAMINATION);

        Witness witness = findWitness(trial.getCurrentCase(), witnessId);

        trial.setTotalActionsCount(trial.getTotalActionsCount() + 1);
        WitnessResponseDTO response = witnessService.interrogate(witness, question, trial.getCurrentCase());

        // Si une contradiction est détectée → action réussie + impact jury
        if (response.isContradictionDetected()) {
            trial.setSuccessfulActionsCount(trial.getSuccessfulActionsCount() + 1);
            verdictService.updateJuryConviction(
                trial.getCurrentCase().getJury(), 0.08, true
            );
            trial.addEvent("🎯 Contradiction révélée sur le témoin " + witness.getName());
        } else {
            trial.addEvent("❓ Question posée à " + witness.getName());
        }

        return response;
    }

    /**
     * Le joueur présente une preuve
     */
    public Trial presentEvidence(String sessionId, String evidenceId) {
        Trial trial = getTrial(sessionId);
        Evidence evidence = findEvidence(trial.getCurrentCase(), evidenceId);

        trial.setTotalActionsCount(trial.getTotalActionsCount() + 1);

        // Polymorphisme : chaque Evidence possède son propre poids
        boolean favorable = !evidence.isAuthentic(); // Preuve non authentique = favorable à la défense
        double impact = evidence.getWeight() * 0.1;

        verdictService.updateJuryConviction(trial.getCurrentCase().getJury(), impact, favorable);

        if (favorable) {
            trial.setSuccessfulActionsCount(trial.getSuccessfulActionsCount() + 1);
            trial.addEvent("✅ Preuve contestée avec succès : " + evidence.getDescription());
        } else {
            trial.addEvent("⚠️ Preuve de l'accusation présentée : " + evidence.getDescription());
        }

        return trial;
    }

    /**
     * Le joueur conteste une preuve
     */
    public Trial contestEvidence(String sessionId, String evidenceId) {
        Trial trial = getTrial(sessionId);
        Evidence evidence = findEvidence(trial.getCurrentCase(), evidenceId);

        evidence.setContested(true);
        trial.setTotalActionsCount(trial.getTotalActionsCount() + 1);

        // Contester une preuve non authentique est un succès
        if (!evidence.isAuthentic()) {
            trial.setSuccessfulActionsCount(trial.getSuccessfulActionsCount() + 1);
            verdictService.updateJuryConviction(trial.getCurrentCase().getJury(), 0.12, true);
            trial.addEvent("🔍 Preuve invalide exposée : " + evidence.getDescription());
        } else {
            trial.addEvent("❌ Contestation rejetée : " + evidence.getDescription());
        }

        return trial;
    }

    /**
     * Confronte deux témoins
     */
    public List<Contradiction> confrontWitnesses(String sessionId, String witnessId1, String witnessId2, String topic) {
        Trial trial = getTrial(sessionId);
        Witness w1 = findWitness(trial.getCurrentCase(), witnessId1);
        Witness w2 = findWitness(trial.getCurrentCase(), witnessId2);

        trial.setTotalActionsCount(trial.getTotalActionsCount() + 1);
        List<Contradiction> contradictions = witnessService.confrontWitnesses(w1, w2, topic);

        if (!contradictions.isEmpty()) {
            trial.setSuccessfulActionsCount(trial.getSuccessfulActionsCount() + 1);
            verdictService.updateJuryConviction(trial.getCurrentCase().getJury(), 0.15, true);
            trial.addEvent("⚡ Confrontation révèle " + contradictions.size() + " contradiction(s) !");
        }

        return contradictions;
    }

    /**
     * Calcule et retourne le verdict final
     */
    public VerdictDTO closeAndGetVerdict(String sessionId) {
        Trial trial = getTrial(sessionId);
        trial.setCurrentPhase(TrialPhase.VERDICT);
        trial.setActive(false);
        trial.setEndTime(new Date());

        VerdictDTO verdict = verdictService.calculateVerdict(trial);
        trial.addEvent("⚖️ Verdict rendu : " + verdict.getVerdict());

        return verdict;
    }

    /**
     * Retourne le niveau de conviction actuel du jury (pour affichage temps réel)
     */
    public double getCurrentJuryConviction(String sessionId) {
        Trial trial = getTrial(sessionId);
        return verdictService.getAverageJuryConviction(trial.getCurrentCase().getJury());
    }

    // ==============================
    //  MÉTHODES UTILITAIRES
    // ==============================

    private Witness findWitness(Case cas, String witnessId) {
        return cas.getWitnesses().stream()
            .filter(w -> w.getName().equalsIgnoreCase(witnessId) || w.getId().equals(witnessId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Témoin introuvable : " + witnessId));
    }

    private Evidence findEvidence(Case cas, String evidenceId) {
        return cas.getEvidences().stream()
            .filter(e -> e.getId().equals(evidenceId) || e.getDescription().equalsIgnoreCase(evidenceId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Preuve introuvable : " + evidenceId));
    }

    private void validatePhase(Trial trial, TrialPhase... allowedPhases) {
        for (TrialPhase phase : allowedPhases) {
            if (trial.getCurrentPhase() == phase) return;
        }
        throw new IllegalStateException("Action non autorisée pendant la phase : " + trial.getCurrentPhase());
    }

    /**
     * Enum des phases du procès
     */
    public enum TrialPhase {
        OPENING_STATEMENTS,   // Déclarations liminaires
        PROSECUTION_CASE,     // Plaidoirie de l'accusation
        DEFENSE_CASE,         // Plaidoirie de la défense (joueur)
        CROSS_EXAMINATION,    // Contre-interrogatoire
        CLOSING_ARGUMENTS,    // Plaidoiries finales
        VERDICT               // Délibération et verdict
    }
}