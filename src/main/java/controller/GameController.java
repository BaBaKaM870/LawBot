package controller;

import dto.*;
import model.*;
import service.TrialService;
import service.VerdictService;
import service.WitnessService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.Random;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private static final Random random = new Random();

    private static final String[] CONTEST_SUCCESS = {
        "Excellente contestation ! Cette preuve est invalide.",
        "Bravo ! Vous venez de pulvériser cette preuve bidon.",
        "Magnifique ! Le procureur blêmit — cette preuve ne tient pas.",
        "Superbe coup ! Même le greffier a failli applaudir.",
        "La défense marque un point. Cette preuve est pure invention.",
    };

    private static final String[] CONTEST_FAIL = {
        "Contestation rejetée. Cette preuve est authentique.",
        "Raté. Cette preuve est bien réelle. Le jury vous regarde avec pitié.",
        "La cour rejette votre contestation. Silence gêné dans la salle.",
        "Bien essayé, mais non. Cette preuve est solide comme un roc.",
        "Votre contestation s'effondre. Le procureur esquisse un sourire.",
    };

    private static final String[] QUESTION_CONTRADICTION = {
        "Contradiction détectée ! ",
        "Le témoin vient de se planter en beauté ! ",
        "Jackpot ! Le témoin se contredit : ",
        "Le tribunal retient son souffle : ",
        "Excellente question ! Une contradiction éclate : ",
    };

    private static final String[] QUESTION_NOTHING = {
        "Réponse enregistrée. Rien de particulier à signaler.",
        "Le témoin répond. Pas de contradiction — continuez d'insister.",
        "Réponse obtenue. Le jury bâille légèrement.",
        "Le témoin s'en sort sans se contredire. Pour l'instant.",
        "Réponse enregistrée. Le juré n°3 griffonne distraitement.",
    };

    private final TrialService  trialService;
    private final WitnessService witnessService;
    private final VerdictService verdictService;

    public GameController(TrialService trialService,
                          WitnessService witnessService,
                          VerdictService verdictService) {
        this.trialService  = trialService;
        this.witnessService = witnessService;
        this.verdictService = verdictService;
    }

    // ── Démarrer une nouvelle partie ─────────────────────────────────────────

    @PostMapping("/new")
    public GameStateDTO newGame() {
        Trial trial = trialService.createNewTrial();
        return toGameState(trial);
    }

    // ── Lire l'état courant ───────────────────────────────────────────────────

    @GetMapping("/{id}")
    public GameStateDTO getState(@PathVariable Long id) {
        return toGameState(trialService.getTrial(id));
    }

    // ── Contester une preuve (par index dans la liste) ───────────────────────

    @PostMapping("/{id}/contest/{idx}")
    public ActionResultDTO contestEvidence(@PathVariable Long id,
                                           @PathVariable int idx) {
        Trial trial = trialService.getTrial(id);
        List<Evidence> evidences = trial.getLawCase().getEvidences();

        if (idx < 0 || idx >= evidences.size())
            return error("Indice de preuve invalide.", trial);

        Evidence evidence = evidences.get(idx);
        if (evidence.isContested())
            return error("Cette preuve est déjà contestée.", trial);

        evidence.setContested(true);
        trial.setTotalActionsCount(trial.getTotalActionsCount() + 1);

        if (!evidence.isAuthentic()) {
            trial.setSuccessfulActionsCount(trial.getSuccessfulActionsCount() + 1);
            verdictService.updateJuryConviction(trial.getLawCase().getJury(), evidence.getWeight(), true);
            trial.addEvent("✔ Fausse preuve exposée : " + evidence.getDescription());
            return new ActionResultDTO(true,
                CONTEST_SUCCESS[random.nextInt(CONTEST_SUCCESS.length)],
                false, null, toGameState(trial));
        } else {
            trial.addEvent("✘ Contestation rejetée : " + evidence.getDescription());
            return new ActionResultDTO(false,
                CONTEST_FAIL[random.nextInt(CONTEST_FAIL.length)],
                false, null, toGameState(trial));
        }
    }

    // ── Interroger un témoin ─────────────────────────────────────────────────

    @PostMapping("/{id}/question/{idx}")
    public ActionResultDTO questionWitness(@PathVariable Long id,
                                           @PathVariable int idx,
                                           @RequestBody Map<String, String> body) {
        Trial trial = trialService.getTrial(id);
        List<Witness> witnesses = trial.getLawCase().getWitnesses();

        if (idx < 0 || idx >= witnesses.size())
            return error("Témoin invalide.", trial);

        String questionText = body.getOrDefault("question", "").trim();
        if (questionText.isEmpty())
            return error("La question ne peut pas être vide.", trial);

        Witness witness = witnesses.get(idx);
        QuestionDTO question = new QuestionDTO(id, null, questionText);
        WitnessResponseDTO response = witnessService.interrogate(witness, question, trial.getLawCase());

        trial.setTotalActionsCount(trial.getTotalActionsCount() + 1);

        if (response.contradictionDetected()) {
            trial.setSuccessfulActionsCount(trial.getSuccessfulActionsCount() + 1);
            verdictService.updateJuryConviction(trial.getLawCase().getJury(), 0.08, true);
            trial.addEvent("⚡ Contradiction révélée : " + witness.getName());
        } else {
            trial.addEvent("Question posée à " + witness.getName());
        }

        String msg = response.contradictionDetected()
            ? QUESTION_CONTRADICTION[random.nextInt(QUESTION_CONTRADICTION.length)] + response.contradictionDescription()
            : QUESTION_NOTHING[random.nextInt(QUESTION_NOTHING.length)];

        return new ActionResultDTO(response.contradictionDetected(), msg,
            response.contradictionDetected(), response.response(), toGameState(trial));
    }

    // ── Confronter deux témoins ───────────────────────────────────────────────

    @PostMapping("/{id}/confront")
    public ActionResultDTO confrontWitnesses(@PathVariable Long id,
                                             @RequestBody Map<String, Object> body) {
        Trial trial = trialService.getTrial(id);
        List<Witness> witnesses = trial.getLawCase().getWitnesses();

        int idx1  = (int) body.get("witness1");
        int idx2  = (int) body.get("witness2");
        String topic = (String) body.getOrDefault("topic", "alibi");

        if (idx1 < 0 || idx1 >= witnesses.size()
                || idx2 < 0 || idx2 >= witnesses.size() || idx1 == idx2)
            return error("Sélection de témoins invalide.", trial);

        Witness w1 = witnesses.get(idx1);
        Witness w2 = witnesses.get(idx2);

        trial.setTotalActionsCount(trial.getTotalActionsCount() + 1);
        List<Contradiction> contradictions = witnessService.confrontWitnesses(w1, w2, topic);

        if (!contradictions.isEmpty()) {
            trial.setSuccessfulActionsCount(trial.getSuccessfulActionsCount() + 1);
            verdictService.updateJuryConviction(trial.getLawCase().getJury(), 0.15, true);
            trial.addEvent("⚡ Confrontation : " + contradictions.size()
                + " contradiction(s) entre " + w1.getName() + " et " + w2.getName());
            return new ActionResultDTO(true,
                contradictions.size() + " contradiction(s) révélée(s) ! "
                    + contradictions.get(0).getDescription(),
                true, null, toGameState(trial));
        } else {
            trial.addEvent("Confrontation sans résultat.");
            return new ActionResultDTO(false,
                "Aucune contradiction détectée. Ces deux témoins se sont visiblement concertés.",
                false, null, toGameState(trial));
        }
    }

    // ── Passer à la phase suivante ────────────────────────────────────────────

    @PostMapping("/{id}/next")
    public GameStateDTO nextPhase(@PathVariable Long id) {
        trialService.advanceTrialPhase(id);
        return toGameState(trialService.getTrial(id));
    }

    // ── Calculer le verdict ───────────────────────────────────────────────────

    @PostMapping("/{id}/verdict")
    public VerdictDTO getVerdict(@PathVariable Long id) {
        return trialService.calculateVerdict(id);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Conversion Trial → GameStateDTO
    // ════════════════════════════════════════════════════════════════════════

    private GameStateDTO toGameState(Trial trial) {
        Case cas = trial.getLawCase();

        List<WitnessInfoDTO> witnesses = new ArrayList<>();
        if (cas.getWitnesses() != null) {
            List<Witness> wList = cas.getWitnesses();
            for (int i = 0; i < wList.size(); i++) {
                Witness w = wList.get(i);
                String stmt = w.getInitialStatement() != null
                    ? w.getInitialStatement().getContent() : null;
                witnesses.add(new WitnessInfoDTO(
                    i, w.getName(), w.getProfession(),
                    w.getCredibility(), w.getStressLevel(), stmt));
            }
        }

        List<EvidenceInfoDTO> evidences = new ArrayList<>();
        if (cas.getEvidences() != null) {
            List<Evidence> eList = cas.getEvidences();
            for (int i = 0; i < eList.size(); i++) {
                Evidence e = eList.get(i);
                evidences.add(new EvidenceInfoDTO(
                    i, e.getDescription(), e.getWeight(),
                    e.isAuthentic(), e.isContested()));
            }
        }

        double juryConviction = verdictService.getAverageJuryConviction(cas.getJury());
        String suspectName = cas.getSuspect() != null ? cas.getSuspect().getName() : "Inconnu";

        return new GameStateDTO(
            trial.getId(),
            trial.getCurrentPhase().name(),
            phaseIndex(trial.getCurrentPhase()),
            cas.getTitle(),
            cas.getCrimeType().name(),
            suspectName,
            cas.getDescription(),
            witnesses,
            evidences,
            juryConviction,
            trial.getSuccessfulActionsCount(),
            trial.getTotalActionsCount(),
            trial.getEvents() != null ? trial.getEvents() : List.of()
        );
    }

    private int phaseIndex(Trial.TrialPhase phase) {
        return switch (phase) {
            case OPENING_STATEMENTS -> 0;
            case PROSECUTION_CASE   -> 1;
            case DEFENSE_CASE       -> 2;
            case CROSS_EXAMINATION  -> 3;
            case CLOSING_ARGUMENTS  -> 4;
            case VERDICT            -> 5;
        };
    }

    private ActionResultDTO error(String message, Trial trial) {
        return new ActionResultDTO(false, message, false, null, toGameState(trial));
    }
}
