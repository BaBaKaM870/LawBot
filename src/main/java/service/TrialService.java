package service;

import dto.QuestionDTO;
import dto.VerdictDTO;
import dto.WitnessResponseDTO;
import model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TrialService {

    private final CaseGeneratorService caseGeneratorService;
    private final WitnessService witnessService;
    private final VerdictService verdictService;

    // Stockage en mémoire des procès actifs (id → Trial)
    private final Map<Long, Trial> activeTrials = new HashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public TrialService(CaseGeneratorService caseGeneratorService,
                        WitnessService witnessService,
                        VerdictService verdictService) {
        this.caseGeneratorService = caseGeneratorService;
        this.witnessService = witnessService;
        this.verdictService = verdictService;
    }

    /** Expose la map pour WitnessService (injection @Lazy). */
    public Map<Long, Trial> getActiveTrials() {
        return activeTrials;
    }

    // ==============================
    //  GESTION DU PROCÈS
    // ==============================

    /** Crée un nouveau procès avec une affaire aléatoire — appelé par TrialController. */
    public Trial createNewTrial() {
        Case newCase = caseGeneratorService.generateRandomCase();
        return createTrial(newCase);
    }

    /** Crée un nouveau procès pour un type de crime donné. */
    public Trial createNewTrial(Case.CrimeType crimeType) {
        Case newCase = caseGeneratorService.generateCaseByType(crimeType);
        return createTrial(newCase);
    }

    private Trial createTrial(Case cas) {
        Trial trial = new Trial();
        Long id = idCounter.getAndIncrement();
        trial.setId(id);
        trial.setLawCase(cas);
        trial.setCurrentPhase(Trial.TrialPhase.OPENING_STATEMENTS);
        trial.setActive(true);
        trial.setSuccessfulActionsCount(0);
        trial.setTotalActionsCount(0);

        activeTrials.put(id, trial);
        // Enregistre les témoins dans WitnessService pour les retrouver par ID
        witnessService.registerWitnesses(cas);
        return trial;
    }

    /** Récupère un procès par son ID — appelé par WitnessService. */
    public Trial getTrial(Long id) {
        Trial trial = activeTrials.get(id);
        if (trial == null) throw new RuntimeException("Aucun procès actif avec l'id : " + id);
        return trial;
    }

    /** Passe à la phase suivante — appelé par TrialController. */
    public void advanceTrialPhase(Long id) {
        Trial trial = getTrial(id);
        Trial.TrialPhase next = getNextPhase(trial.getCurrentPhase());
        trial.setCurrentPhase(next);
        if (next == Trial.TrialPhase.VERDICT) {
            trial.setActive(false);
        }
    }

    /** Calcule et retourne le verdict final — appelé par TrialController. */
    public VerdictDTO calculateVerdict(Long id) {
        Trial trial = getTrial(id);
        trial.setCurrentPhase(Trial.TrialPhase.VERDICT);
        trial.setActive(false);
        VerdictDTO verdict = verdictService.calculateVerdict(trial);
        trial.addEvent("Verdict rendu : " + verdict.status());
        return verdict;
    }

    // ==============================
    //  ACTIONS PENDANT LE PROCÈS
    // ==============================

    /** Interroge un témoin pendant le procès. */
    public WitnessResponseDTO questionWitness(Long trialId, Long witnessId, QuestionDTO question) {
        Trial trial = getTrial(trialId);
        validatePhase(trial, Trial.TrialPhase.DEFENSE_CASE, Trial.TrialPhase.CROSS_EXAMINATION);

        Witness witness = findWitness(trial.getLawCase(), witnessId);

        trial.setTotalActionsCount(trial.getTotalActionsCount() + 1);
        WitnessResponseDTO response = witnessService.interrogate(witness, question, trial.getLawCase());

        if (response.contradictionDetected()) {
            trial.setSuccessfulActionsCount(trial.getSuccessfulActionsCount() + 1);
            verdictService.updateJuryConviction(trial.getLawCase().getJury(), 0.08, true);
            trial.addEvent("Contradiction révélée sur le témoin " + witness.getName());
        } else {
            trial.addEvent("Question posée à " + witness.getName());
        }

        return response;
    }

    /** Présente une preuve au jury — appelé par EvidenceController. */
    public boolean presentEvidenceToJury(Long trialId, Long evidenceId) {
        Trial trial = getTrial(trialId);
        Evidence evidence = findEvidence(trial.getLawCase(), evidenceId);

        trial.setTotalActionsCount(trial.getTotalActionsCount() + 1);

        boolean favorable = !evidence.isAuthentic();
        double impact = evidence.getWeight();

        verdictService.updateJuryConviction(trial.getLawCase().getJury(), impact, favorable);

        if (favorable) {
            trial.setSuccessfulActionsCount(trial.getSuccessfulActionsCount() + 1);
            trial.addEvent("Preuve favorable présentée : " + evidence.getDescription());
        } else {
            trial.addEvent("Preuve de l'accusation présentée : " + evidence.getDescription());
        }

        return favorable;
    }

    /** Retourne les preuves d'une affaire par son ID — appelé par EvidenceController. */
    public List<Evidence> getEvidencesForCase(Long caseId) {
        return activeTrials.values().stream()
            .map(Trial::getLawCase)
            .filter(c -> caseId.equals(c.getId()))
            .findFirst()
            .map(Case::getEvidences)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + caseId));
    }

    /** Conteste une preuve. */
    public Trial contestEvidence(Long trialId, Long evidenceId) {
        Trial trial = getTrial(trialId);
        Evidence evidence = findEvidence(trial.getLawCase(), evidenceId);

        evidence.setContested(true);
        trial.setTotalActionsCount(trial.getTotalActionsCount() + 1);

        if (!evidence.isAuthentic()) {
            trial.setSuccessfulActionsCount(trial.getSuccessfulActionsCount() + 1);
            verdictService.updateJuryConviction(trial.getLawCase().getJury(), 0.12, true);
            trial.addEvent("Preuve invalide exposée : " + evidence.getDescription());
        } else {
            trial.addEvent("Contestation rejetée : " + evidence.getDescription());
        }

        return trial;
    }

    /** Confronte deux témoins. */
    public List<Contradiction> confrontWitnesses(Long trialId, Long witnessId1, Long witnessId2, String topic) {
        Trial trial = getTrial(trialId);
        Witness w1 = findWitness(trial.getLawCase(), witnessId1);
        Witness w2 = findWitness(trial.getLawCase(), witnessId2);

        trial.setTotalActionsCount(trial.getTotalActionsCount() + 1);
        List<Contradiction> contradictions = witnessService.confrontWitnesses(w1, w2, topic);

        if (!contradictions.isEmpty()) {
            trial.setSuccessfulActionsCount(trial.getSuccessfulActionsCount() + 1);
            verdictService.updateJuryConviction(trial.getLawCase().getJury(), 0.15, true);
            trial.addEvent("Confrontation révèle " + contradictions.size() + " contradiction(s) !");
        }

        return contradictions;
    }

    // ==============================
    //  MÉTHODES UTILITAIRES
    // ==============================

    private Trial.TrialPhase getNextPhase(Trial.TrialPhase current) {
        return switch (current) {
            case OPENING_STATEMENTS -> Trial.TrialPhase.PROSECUTION_CASE;
            case PROSECUTION_CASE   -> Trial.TrialPhase.DEFENSE_CASE;
            case DEFENSE_CASE       -> Trial.TrialPhase.CROSS_EXAMINATION;
            case CROSS_EXAMINATION  -> Trial.TrialPhase.CLOSING_ARGUMENTS;
            case CLOSING_ARGUMENTS  -> Trial.TrialPhase.VERDICT;
            case VERDICT            -> Trial.TrialPhase.VERDICT;
        };
    }

    private Witness findWitness(Case cas, Long witnessId) {
        return cas.getWitnesses().stream()
            .filter(w -> witnessId.equals(w.getId()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Témoin introuvable : " + witnessId));
    }

    private Evidence findEvidence(Case cas, Long evidenceId) {
        return cas.getEvidences().stream()
            .filter(e -> evidenceId.equals(e.getId()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Preuve introuvable : " + evidenceId));
    }

    private void validatePhase(Trial trial, Trial.TrialPhase... allowedPhases) {
        for (Trial.TrialPhase phase : allowedPhases) {
            if (trial.getCurrentPhase() == phase) return;
        }
        throw new IllegalStateException("Action non autorisée pendant la phase : " + trial.getCurrentPhase());
    }
}
