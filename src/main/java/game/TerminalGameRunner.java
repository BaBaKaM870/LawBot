package game;

import dto.QuestionDTO;
import dto.VerdictDTO;
import dto.WitnessResponseDTO;
import model.*;
import service.CaseGeneratorService;
import service.VerdictService;
import service.WitnessService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

// @Component  ← désactivé : mode web actif. Remettre pour jouer en terminal.
public class TerminalGameRunner implements CommandLineRunner {

    // ── Codes ANSI ──────────────────────────────────────────────────────────
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE   = "\u001B[34m";
    private static final String CYAN   = "\u001B[36m";
    private static final String WHITE  = "\u001B[37m";

    // ── Services ─────────────────────────────────────────────────────────────
    private final CaseGeneratorService caseGeneratorService;
    private final VerdictService       verdictService;
    private final WitnessService       witnessService;

    // ── État de la partie ────────────────────────────────────────────────────
    private Scanner scanner;
    private Case    currentCase;
    private Trial   currentTrial;

    public TerminalGameRunner(CaseGeneratorService caseGeneratorService,
                              VerdictService verdictService,
                              @Lazy WitnessService witnessService) {
        this.caseGeneratorService = caseGeneratorService;
        this.verdictService       = verdictService;
        this.witnessService       = witnessService;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Point d'entrée CommandLineRunner
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void run(String... args) {
        scanner = new Scanner(System.in);
        showWelcome();

        boolean playing = true;
        while (playing) {
            startNewGame();
            playing = askReplay();
        }

        System.out.println(CYAN + "\nMerci d'avoir joué à LawBot ! Consultez un vrai avocat pour vos vrais problèmes." + RESET);
        scanner.close();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Écran d'accueil
    // ════════════════════════════════════════════════════════════════════════
    private void showWelcome() {
        clearScreen();
        System.out.println(YELLOW + BOLD);
        System.out.println("  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║    ⚖   LAWBOT  -  SIMULATION DE PROCÈS  ⚖   ║");
        System.out.println("  ║     Vous êtes l'avocat de la défense...      ║");
        System.out.println("  ║         (bonne chance, vraiment)             ║");
        System.out.println("  ╚══════════════════════════════════════════════╝");
        System.out.println(RESET);
        System.out.println(WHITE
            + "  Votre mission : défendre votre client et obtenir un acquittement !\n"
            + "  Interrogez les témoins, contestez les preuves, convainquez le jury.\n"
            + RESET);
        System.out.println(CYAN
            + "  COMMANDES :\n"
            + "    contest [N]             - Contester la preuve N\n"
            + "    question [N] [texte]    - Interroger le témoin N\n"
            + "    confront [N1] [N2] [sujet] - Confronter deux témoins\n"
            + "    next                    - Passer à la phase suivante\n"
            + RESET);
        waitEnter("  Appuyez sur ENTRÉE pour commencer... (votre café refroidit déjà)");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Déroulement d'une partie
    // ════════════════════════════════════════════════════════════════════════
    private void startNewGame() {
        currentCase  = caseGeneratorService.generateRandomCase();
        currentTrial = new Trial();
        currentTrial.setLawCase(currentCase);
        currentTrial.setCurrentPhase(Trial.TrialPhase.OPENING_STATEMENTS);
        currentTrial.setActive(true);
        currentTrial.setSuccessfulActionsCount(0);
        currentTrial.setTotalActionsCount(0);

        phaseOpening();
        phaseProsecution();
        phaseDefense();
        phaseCrossExamination();
        phaseClosing();
        phaseVerdict();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Phase 1 – Déclarations d'ouverture
    // ════════════════════════════════════════════════════════════════════════
    private void phaseOpening() {
        clearScreen();
        printPhaseHeader(1, "DÉCLARATIONS D'OUVERTURE");

        Suspect suspect = currentCase.getSuspect();
        System.out.println(BOLD + "  Affaire    : " + RESET + YELLOW + currentCase.getTitle() + RESET);
        System.out.println(BOLD + "  Crime      : " + RESET + formatCrimeType(currentCase.getCrimeType()));
        System.out.println(BOLD + "  Accusé(e)  : " + RESET
            + (suspect != null ? suspect.getName() : "Inconnu"));
        System.out.println();
        System.out.println(BOLD + "  Description :" + RESET);
        System.out.println("  " + currentCase.getDescription());
        System.out.println();

        printWitnessList();
        System.out.println();
        printEvidenceList();

        waitEnter("\n  Appuyez sur ENTRÉE pour passer à la phase d'accusation...");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Phase 2 – Cas de l'accusation
    // ════════════════════════════════════════════════════════════════════════
    private void phaseProsecution() {
        clearScreen();
        printPhaseHeader(2, "CAS DE L'ACCUSATION");
        System.out.println(WHITE
            + "  L'accusation présente ses preuves avec un grand sourire satisfait.\n"
            + "  Certaines sont bidons — à vous de les débusquer !\n"
            + RESET);

        int actionsLeft = 3;
        while (actionsLeft > 0) {
            printEvidenceList();
            System.out.println();
            printActions(actionsLeft,
                "contest [N]  - Contester la preuve N",
                "next         - Passer à la phase de défense");
            System.out.print(BOLD + "  > " + RESET);

            String input = scanner.nextLine().trim().toLowerCase();
            System.out.println();

            if (input.equals("next")) break;

            if (input.startsWith("contest ")) {
                String[] parts = input.split(" ", 2);
                try {
                    int idx = Integer.parseInt(parts[1].trim()) - 1;
                    List<Evidence> evidences = currentCase.getEvidences();
                    if (idx < 0 || idx >= evidences.size()) {
                        System.out.println(RED + "  Numéro de preuve invalide." + RESET);
                        continue;
                    }
                    Evidence e = evidences.get(idx);
                    if (e.isContested()) {
                        System.out.println(YELLOW + "  Cette preuve est déjà contestée." + RESET);
                        continue;
                    }
                    e.setContested(true);
                    currentTrial.setTotalActionsCount(currentTrial.getTotalActionsCount() + 1);
                    if (!e.isAuthentic()) {
                        currentTrial.setSuccessfulActionsCount(
                            currentTrial.getSuccessfulActionsCount() + 1);
                        verdictService.updateJuryConviction(
                            currentCase.getJury(), e.getWeight(), true);
                        System.out.println(GREEN
                            + "  ✔ Excellente contestation ! \"" + e.getDescription()
                            + "\" est une fausse preuve." + RESET);
                        System.out.println(GREEN + "  Le jury est influencé en votre faveur." + RESET);
                    } else {
                        System.out.println(RED
                            + "  ✘ Contestation rejetée. Cette preuve est authentique." + RESET);
                    }
                    actionsLeft--;
                } catch (NumberFormatException ex) {
                    System.out.println(RED + "  Usage : contest [numéro]  Ex: contest 2" + RESET);
                }
            } else {
                System.out.println(RED + "  Commande non reconnue." + RESET);
            }
        }
        System.out.println();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Phase 3 – Cas de la défense
    // ════════════════════════════════════════════════════════════════════════
    private void phaseDefense() {
        clearScreen();
        printPhaseHeader(3, "CAS DE LA DÉFENSE");
        System.out.println(WHITE
            + "  Interrogez les témoins pour semer le doute !\n"
            + "  Poser des questions bêtes peut parfois révéler de grandes vérités.\n"
            + RESET);

        int actionsLeft = 5;
        while (actionsLeft > 0) {
            printWitnessList();
            System.out.println();
            printActions(actionsLeft,
                "question [N] [texte]  - Interroger le témoin N",
                "next                  - Passer au contre-interrogatoire");
            System.out.print(BOLD + "  > " + RESET);

            String input = scanner.nextLine().trim();
            System.out.println();

            if (input.equalsIgnoreCase("next")) break;

            if (input.toLowerCase().startsWith("question ")) {
                String[] parts = input.split(" ", 3);
                if (parts.length < 3) {
                    System.out.println(RED + "  Usage : question [N] [texte]  Ex: question 1 Où étiez-vous ?" + RESET);
                    continue;
                }
                try {
                    int idx = Integer.parseInt(parts[1].trim()) - 1;
                    List<Witness> witnesses = currentCase.getWitnesses();
                    if (idx < 0 || idx >= witnesses.size()) {
                        System.out.println(RED + "  Numéro de témoin invalide." + RESET);
                        continue;
                    }
                    Witness witness = witnesses.get(idx);
                    String questionText = parts[2];
                    QuestionDTO question = new QuestionDTO(1L, null, questionText);

                    WitnessResponseDTO response =
                        witnessService.interrogate(witness, question, currentCase);
                    currentTrial.setTotalActionsCount(
                        currentTrial.getTotalActionsCount() + 1);

                    System.out.println(YELLOW + BOLD
                        + "  [" + response.witnessName() + " répond]" + RESET);
                    System.out.println("  \"" + response.response() + "\"");
                    System.out.printf("  Crédibilité : %d/100  |  Stress : %.0f%%%n",
                        response.credibility(),
                        witness.getStressLevel() * 100);

                    if (response.contradictionDetected()) {
                        System.out.println(GREEN + "\n  ★ CONTRADICTION DÉTECTÉE !" + RESET);
                        System.out.println(GREEN + "  " + response.contradictionDescription() + RESET);
                        currentTrial.setSuccessfulActionsCount(
                            currentTrial.getSuccessfulActionsCount() + 1);
                        verdictService.updateJuryConviction(
                            currentCase.getJury(), 0.08, true);
                    }
                    actionsLeft--;
                } catch (NumberFormatException ex) {
                    System.out.println(RED + "  Usage : question [N] [texte]  Ex: question 1 Où étiez-vous ?" + RESET);
                }
            } else {
                System.out.println(RED + "  Commande non reconnue." + RESET);
            }
            System.out.println();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Phase 4 – Contre-interrogatoire
    // ════════════════════════════════════════════════════════════════════════
    private void phaseCrossExamination() {
        clearScreen();
        printPhaseHeader(4, "CONTRE-INTERROGATOIRE");
        System.out.println(WHITE
            + "  Confrontez deux témoins sur le même sujet !\n"
            + "  Des contradictions entre eux affaibliront l'accusation — et leur amitié.\n"
            + RESET);

        int actionsLeft = 2;
        while (actionsLeft > 0) {
            printWitnessList();
            System.out.println();
            printActions(actionsLeft,
                "confront [N1] [N2] [sujet]  - Confronter deux témoins",
                "next                         - Passer aux plaidoiries");
            System.out.print(BOLD + "  > " + RESET);

            String input = scanner.nextLine().trim();
            System.out.println();

            if (input.equalsIgnoreCase("next")) break;

            if (input.toLowerCase().startsWith("confront ")) {
                String[] parts = input.split(" ", 4);
                if (parts.length < 4) {
                    System.out.println(RED + "  Usage : confront [N1] [N2] [sujet]  Ex: confront 1 2 alibi" + RESET);
                    continue;
                }
                try {
                    int idx1 = Integer.parseInt(parts[1].trim()) - 1;
                    int idx2 = Integer.parseInt(parts[2].trim()) - 1;
                    String topic = parts[3];
                    List<Witness> witnesses = currentCase.getWitnesses();

                    if (idx1 < 0 || idx1 >= witnesses.size()
                            || idx2 < 0 || idx2 >= witnesses.size()
                            || idx1 == idx2) {
                        System.out.println(RED + "  Numéros invalides (doivent être différents et dans la liste)." + RESET);
                        continue;
                    }
                    Witness w1 = witnesses.get(idx1);
                    Witness w2 = witnesses.get(idx2);

                    currentTrial.setTotalActionsCount(
                        currentTrial.getTotalActionsCount() + 1);
                    List<Contradiction> contradictions =
                        witnessService.confrontWitnesses(w1, w2, topic);

                    if (!contradictions.isEmpty()) {
                        System.out.println(GREEN + "\n  ★ "
                            + contradictions.size()
                            + " CONTRADICTION(S) RÉVÉLÉE(S) !" + RESET);
                        for (Contradiction c : contradictions) {
                            System.out.println(GREEN + "  → " + c.getDescription() + RESET);
                        }
                        currentTrial.setSuccessfulActionsCount(
                            currentTrial.getSuccessfulActionsCount() + 1);
                        verdictService.updateJuryConviction(
                            currentCase.getJury(), 0.15, true);
                    } else {
                        System.out.println(YELLOW
                            + "  Aucune contradiction détectée entre ces témoins sur ce sujet." + RESET);
                    }
                    actionsLeft--;
                } catch (NumberFormatException ex) {
                    System.out.println(RED + "  Usage : confront [N1] [N2] [sujet]  Ex: confront 1 2 alibi" + RESET);
                }
            } else {
                System.out.println(RED + "  Commande non reconnue." + RESET);
            }
            System.out.println();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Phase 5 – Plaidoiries finales
    // ════════════════════════════════════════════════════════════════════════
    private void phaseClosing() {
        clearScreen();
        printPhaseHeader(5, "PLAIDOIRIES FINALES");
        System.out.println(WHITE
            + "  C'est votre dernière chance de convaincre le jury.\n"
            + "  Le juré n°4 a l'air d'avoir faim, soyez convaincant et rapide.\n"
            + "  Entrez votre plaidoirie (ou ENTRÉE pour une standard) :\n"
            + RESET);
        System.out.print(BOLD + "  > " + RESET);

        String plea = scanner.nextLine().trim();
        System.out.println();
        if (!plea.isEmpty()) {
            System.out.println(CYAN + "  Votre plaidoirie :" + RESET);
            System.out.println("  \"" + plea + "\"");
            System.out.println(GREEN + "\n  Le jury a pris note. Le juré n°2 hoche la tête lentement." + RESET);
        } else {
            System.out.println(YELLOW + "  Plaidoirie standard prononcée. Le jury hausse collectivement les épaules." + RESET);
        }

        waitEnter("\n  Appuyez sur ENTRÉE pour le verdict...");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Phase 6 – Verdict
    // ════════════════════════════════════════════════════════════════════════
    private void phaseVerdict() {
        clearScreen();
        printPhaseHeader(6, "VERDICT");

        currentTrial.setCurrentPhase(Trial.TrialPhase.VERDICT);
        currentTrial.setActive(false);

        VerdictDTO verdict = verdictService.calculateVerdict(currentTrial);

        System.out.println(YELLOW + BOLD);
        System.out.println("  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║               VERDICT DU JURY                ║");
        System.out.println("  ╚══════════════════════════════════════════════╝");
        System.out.println(RESET);

        System.out.println(BOLD + "  Accusé(e) : " + RESET + verdict.suspectName());
        System.out.println();

        if ("NOT_GUILTY".equals(verdict.status())) {
            System.out.println(GREEN + BOLD + "  ✔  VERDICT : NON COUPABLE !" + RESET);
            System.out.println(GREEN + "     Votre client est libéré. Il ne vous remerciera probablement pas assez." + RESET);
        } else {
            System.out.println(RED + BOLD + "  ✘  VERDICT : COUPABLE" + RESET);
            System.out.println(RED + "     Votre client est condamné. Il vous en voudra pour toujours." + RESET);
        }
        System.out.println();

        System.out.println(BOLD + "  Votes du jury   : " + RESET
            + verdict.guiltyVotes() + "/" + verdict.totalJurors() + " pour la culpabilité");

        String scoreColor = verdict.playerScore() >= 70 ? GREEN
                          : verdict.playerScore() >= 40 ? YELLOW : RED;
        System.out.println(BOLD + "  Score défenseur : " + RESET
            + scoreColor + verdict.playerScore() + "/100" + RESET);

        System.out.println(BOLD + "  Réellement coupable ? " + RESET
            + (verdict.wasActuallyGuilty()
                ? RED + "OUI — vous défendiez un(e) coupable." + RESET
                : GREEN + "NON — votre client était innocent !" + RESET));

        System.out.println();
        System.out.println(WHITE + "  " + verdict.explanation() + RESET);

        System.out.println();
        System.out.println(BOLD + "  Actions réussies : " + RESET
            + currentTrial.getSuccessfulActionsCount()
            + "/" + currentTrial.getTotalActionsCount());
        System.out.println();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Méthodes utilitaires
    // ════════════════════════════════════════════════════════════════════════

    private void printPhaseHeader(int phase, String name) {
        System.out.println(BLUE + BOLD);
        System.out.println("  ══════════════════════════════════════════════");
        System.out.printf ("  Phase %d/6 : %s%n", phase, name);
        System.out.println("  ══════════════════════════════════════════════");
        System.out.println(RESET);
    }

    private void printActions(int actionsLeft, String... actions) {
        System.out.println(CYAN + "  Actions disponibles (" + actionsLeft + " restante(s)) :" + RESET);
        for (String a : actions) {
            System.out.println("    " + a);
        }
        System.out.println();
    }

    private void printWitnessList() {
        System.out.println(BOLD + "  Témoins :" + RESET);
        List<Witness> witnesses = currentCase.getWitnesses();
        for (int i = 0; i < witnesses.size(); i++) {
            Witness w = witnesses.get(i);
            String credColor = w.getCredibility() >= 70 ? GREEN
                             : w.getCredibility() >= 40 ? YELLOW : RED;
            System.out.printf("    [%d] %-20s (%s)%n", i + 1,
                w.getName(), w.getProfession());
            System.out.printf("        Crédibilité : %s%d/100%s  |  Stress : %.0f%%%n",
                credColor, w.getCredibility(), RESET,
                w.getStressLevel() * 100);
            if (w.getInitialStatement() != null) {
                System.out.println("        \"" + w.getInitialStatement().getContent() + "\"");
            }
        }
    }

    private void printEvidenceList() {
        System.out.println(BOLD + "  Preuves :" + RESET);
        List<Evidence> evidences = currentCase.getEvidences();
        for (int i = 0; i < evidences.size(); i++) {
            Evidence e = evidences.get(i);
            String authColor = e.isAuthentic() ? RED : GREEN;
            String authLabel = e.isAuthentic() ? "[AUTHENTIQUE]" : "[DOUTEUX]   ";
            String contested  = e.isContested() ? YELLOW + "  [CONTESTÉ]" + RESET : "";
            System.out.printf("    [%d] %-40s Force : %.0f%%  %s%s%s%n",
                i + 1, e.getDescription(), e.getWeight() * 100,
                authColor, authLabel, RESET + contested);
        }
    }

    private void waitEnter(String message) {
        System.out.println(WHITE + message + RESET);
        scanner.nextLine();
    }

    private boolean askReplay() {
        System.out.print(BOLD + "  Voulez-vous rejouer ? (o/n) : " + RESET);
        String answer = scanner.nextLine().trim().toLowerCase();
        return answer.equals("o") || answer.equals("oui")
            || answer.equals("y") || answer.equals("yes");
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private String formatCrimeType(Case.CrimeType type) {
        return switch (type) {
            case MURDER     -> "Meurtre";
            case FRAUD      -> "Fraude";
            case THEFT      -> "Vol";
            case ASSAULT    -> "Agression";
            case CORRUPTION -> "Corruption";
        };
    }
}
