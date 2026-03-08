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
    private static final Random random = new Random();

    private static final String[] STRESSED_RESPONSES = {
        "En fait... %s Enfin, je crois. Mon cerveau disjoncte sous pression.",
        "Je... %s Du moins c'est ce que je pense. Quelqu'un a de l'eau ?",
        "Voyez-vous... %s Mais là je suis un peu à plat. Très stressé, quoi.",
        "Euh... %s Attendez, j'ai dit ça ? Mon avocat va m'en vouloir.",
        "Pour être honnête... %s Mais honnêtement, j'ai surtout très peur là.",
        "Je maintiens que... %s Enfin peut-être. Je ne dors plus très bien ces temps-ci.",
    };

    // ─── Réponses contextuelles par type de question ────────────────
    // Dimensions : [personnalité (0=NERVOUS,1=LIAR,2=CONFIDENT,3=COOPERATIVE)][variante (0-2)]
    // %s = contenu de la déclaration initiale du témoin

    // Chaque tableau : [personnalité][variante]
    // Variante 0 : contient %s (déclaration initiale) — utilisée pour la 1ère question
    // Variantes 1 & 2 : PAS de %s — utilisées pour les questions suivantes (réponses variées)

    private static final String[][] ALIBI_RESPONSES = {
        { // NERVOUS
            "Mon alibi ? Je l'ai dit clairement : %s — et je ne comprends pas pourquoi on me demande encore.",
            "Encore cette question sur l'alibi ! J'étais ailleurs, loin des faits. Point. Pourquoi je tremble ? C'est la climatisation.",
            "Je vous l'ai dit, je vous le redis, je ne sais pas combien de fois il faudra le répéter. Je N'ÉTAIS PAS LÀ. (Pardon pour le ton.)"
        }, { // LIAR
            "Mon alibi est clair : %s — je n'ai rien à cacher, c'est précisément pour ça que je suis ici.",
            "Mon alibi est bétonné et vérifiable. Posez des questions concrètes, vous trouverez la même réponse.",
            "Je trouve curieux qu'on insiste autant sur un alibi que j'ai déjà expliqué en détail. Cela semble indiquer un manque d'éléments de votre côté."
        }, { // CONFIDENT
            "%s — là est mon alibi. Je ne vois pas l'ambiguïté.",
            "La réponse est la même depuis le début. Mon alibi est solide, documenté, vérifiable. Je commence à me lasser.",
            "On tourne en rond. Mon alibi tient debout. Vos questions répétées n'en changeront pas les faits."
        }, { // COOPERATIVE
            "Je peux tout à fait l'expliquer. %s C'est tout, rien de plus.",
            "Sur ma présence ce soir-là : j'ai fourni tous les détails à la police. Je peux les répéter si nécessaire.",
            "Mon emploi du temps est parfaitement clair. Je coopère pleinement depuis le début de cette affaire."
        }
    };

    private static final String[][] CHALLENGE_RESPONSES = {
        { // NERVOUS
            "Je… je ne mens pas ! %s Enfin, c'est ce dont je me souviens. La vérité, c'est… compliqué.",
            "Vous m'accusez de mentir ? (Longue pause) Je suis... blessé. Et nerveux. Mais surtout blessé.",
            "Si je mentais, je… je ferais quoi exactement ? Non, je dis la vérité. Je le jure sur quelque chose d'important."
        }, { // LIAR
            "Je suis profondément offensé. %s Tout ce que j'ai dit est absolument véridique, je le jure sur beaucoup de choses.",
            "Moi, mentir ? Ici, au tribunal, sous serment ? C'est une accusation grave. Mon avocat va adorer cette question.",
            "Je n'ai aucune raison de mentir. Zéro. Aucun intérêt. Contrairement à certaines personnes ici présentes que je ne nommerai pas."
        }, { // CONFIDENT
            "Vous insinuez que je mens ? %s Je vous suggère de relire vos notes avant d'accuser quelqu'un de ma trempe.",
            "Vous m'accusez de mentir ? Alors prouvez-le. Je vous attends. (Silence)",
            "Remarquable. Quand les preuves manquent, on attaque le témoin. Procédé classique. Je ne me laisserai pas intimider."
        }, { // COOPERATIVE
            "Je comprends votre doute, mais : %s C'est exactement ce que j'ai vécu, sans rien déformer.",
            "Je n'ai aucune raison de mentir à ce tribunal. Je suis ici pour aider, pas pour compliquer les choses.",
            "Si vous pensez que je mens, vérifiez mes déclarations point par point. Je n'ai rien à cacher."
        }
    };

    private static final String[][] RELATIONSHIP_RESPONSES = {
        { // NERVOUS
            "Notre… notre relation ? %s Nous n'étions pas très proches, je vous l'assure. Enfin, pas trop.",
            "On se connaissait, oui. Pas intimement. Vaguement. Professionnellement. C'est tout. (Je sue légèrement.)",
            "Je ne dirais pas qu'on était amis. On s'est croisés. Ça arrive. Ce n'est pas un crime de croiser des gens, n'est-ce pas ?"
        }, { // LIAR
            "Nos relations étaient purement professionnelles. %s Je le connaissais à peine en dehors du travail.",
            "On se connaissait à peine. Je ne vois pas en quoi notre relation a un quelconque rapport avec cette affaire.",
            "Je fréquente beaucoup de gens dans mon activité. Ça ne veut rien dire de particulier."
        }, { // CONFIDENT
            "Ma relation avec cette personne ? %s C'est la réponse, complète et définitive.",
            "Relation strictement professionnelle. Aucune amitié, aucun conflit personnel. C'est la réponse, et elle ne changera pas.",
            "On se connaissait dans un cadre précis. Rien d'autre. Je ne vois pas pourquoi vous cherchez autre chose."
        }, { // COOPERATIVE
            "Sur notre relation : %s Je peux expliquer le contexte si vous le souhaitez.",
            "Je peux vous décrire exactement le cadre de notre relation. Purement professionnel, sans tension particulière.",
            "On se connaissait depuis peu, dans un contexte précis. Je suis ouvert à toute question complémentaire là-dessus."
        }
    };

    private static final String[][] OBSERVATION_RESPONSES = {
        { // NERVOUS
            "J'ai vu… j'ai cru voir… %s Mais la lumière était particulière et j'avais un peu faim.",
            "J'ai vu des choses, oui. Mes yeux fonctionnent normalement. La plupart du temps. Ce soir-là... conditions difficiles.",
            "Je ne remets pas en cause ce que j'ai vu. Mais entre voir et jurer, il y a une nuance. Je préfère être honnête."
        }, { // LIAR
            "J'ai tout observé avec grande attention. %s Chaque détail est exact — j'ai une excellente mémoire.",
            "Mes observations sont précises et non contestables. J'étais parfaitement positionné pour voir ce que j'ai décrit.",
            "J'ai vu ce que j'ai vu. Clairement, distinctement. Personne ne peut me faire dire le contraire."
        }, { // CONFIDENT
            "Mes observations sont sans appel. %s Et j'avais mes lunettes, contrairement à certains.",
            "Je vois que vous essayez de fragiliser mon témoignage. Mes observations sont claires et je les maintiens.",
            "J'ai observé les faits avec attention. Ce que j'ai décrit correspond exactement à ce que j'ai vu. Fin de discussion."
        }, { // COOPERATIVE
            "Je vais être précis sur ce point : %s J'ai fait de mon mieux pour être exact.",
            "Je fais de mon mieux pour être précis. Ce que j'ai observé correspond à ce que j'ai décrit dans ma déposition.",
            "Mes observations sont ce qu'elles sont. Je ne peux ni les amplifier ni les minimiser. La vérité, simplement."
        }
    };

    private static final String[][] TIMELINE_RESPONSES = {
        { // NERVOUS
            "L'heure ? %s Mais j'avais regardé ma montre — à 10 minutes près, peut-être.",
            "L'heure précise... je dirais... environ... vers... Ce n'est pas facile de se souvenir exactement quand on est stressé.",
            "Vous voulez l'heure exacte ? Mon téléphone était déchargé. Et ma montre retarde. Disons... le soir."
        }, { // LIAR
            "L'heure exacte ? %s Je l'ai bien retenu, c'était précisément ça.",
            "J'ai une excellente notion du temps. L'heure que j'ai indiquée est correcte, je n'ai aucun doute là-dessus.",
            "Le timing est parfaitement clair dans ma mémoire. Je n'ai pas l'habitude d'inventer des horaires."
        }, { // CONFIDENT
            "L'heure ? %s Ma montre était parfaitement réglée, je n'ai aucun doute.",
            "Les faits se sont produits exactement à l'heure que j'ai indiquée. Je suis quelqu'un de ponctuel.",
            "Je n'ai pas de doute sur le timing. Ce qui s'est passé s'est passé quand je l'ai dit. C'est comme ça."
        }, { // COOPERATIVE
            "Sur le timing : %s J'avais regardé l'heure juste avant, je peux le confirmer.",
            "Je peux confirmer l'heure approximative. Je ne suis pas sûr à la minute près, mais la fenêtre temporelle est correcte.",
            "Le timing est cohérent avec ce que j'ai observé. Si vous avez des éléments qui contredisent l'heure, je suis preneur."
        }
    };

    private static final String[][] MOTIVE_RESPONSES = {
        { // NERVOUS
            "Mon motif ? %s Je n'avais aucune raison de… de quoi au juste ? Je suis innocent de tout ça.",
            "Pourquoi je témoigne ? Parce qu'on me l'a demandé ! Et que c'est mon devoir. Et que... voilà.",
            "Je n'ai aucun intérêt personnel ici. Aucun. Si j'en avais un, je ne serais probablement pas aussi nerveux."
        }, { // LIAR
            "Je n'ai aucun intérêt personnel dans cette affaire. %s Je témoigne en toute objectivité.",
            "Mon mobile ? Je n'en ai pas. Certaines personnes témoignent par sens civique. C'est mon cas.",
            "Je suis ici parce que j'ai vu quelque chose et que j'ai le devoir de le dire. Pas plus, pas moins."
        }, { // CONFIDENT
            "Ma motivation ? %s Je n'ai pas de mobile caché — si j'en avais un, je le garderais mieux caché.",
            "Je n'ai rien à gagner dans ce procès. Absolument rien. Ce qui signifie que je n'ai aucune raison de mentir.",
            "Mon mobile est simple : la vérité. Concept peut-être étranger à certains, mais je m'y tiens."
        }, { // COOPERATIVE
            "Mon mobile est transparent : %s Je n'ai rien à gagner à mentir ici.",
            "Je témoigne parce que ce que j'ai vu me semblait important à signaler. C'est tout. Pas de complication.",
            "Je n'ai aucun lien personnel avec les parties. Je suis ici pour relater ce dont j'ai été témoin, rien de plus."
        }
    };

    private static final String[][] GENERIC_RESPONSES = {
        { // NERVOUS
            "Euh… %s C'est la réponse. Je crois. Je transpire légèrement.",
            "C'est... une bonne question. Vraiment. Je réfléchis. (Silence) Ma réponse est que... c'est compliqué.",
            "Je ne suis pas sûr de comprendre ce que vous me demandez. Mais quelle que soit la réponse, je n'ai rien fait de mal."
        }, { // LIAR
            "La réponse à votre question est simple : %s Je ne vois pas où vous voulez en venir.",
            "Ma réponse est la même depuis le début. Je ne changerai pas de position sous la pression de questions répétées.",
            "Je pense que vous tournez en rond. Mes déclarations sont claires, cohérentes, et je les maintiens entièrement."
        }, { // CONFIDENT
            "%s — voilà la réponse. Question suivante.",
            "Je pourrais répondre à cette question toute la journée. La réponse ne changera pas. Passez à autre chose.",
            "Vous cherchez une faille ? Bonne chance. Mes déclarations sont imperméables."
        }, { // COOPERATIVE
            "Je comprends votre question. %s Je reste à disposition pour d'autres précisions.",
            "Je veux être aussi utile que possible. Si ma réponse précédente n'était pas claire, je suis prêt à reformuler.",
            "J'essaie d'être le plus précis possible. Si vous avez des éléments spécifiques, je suis là."
        }
    };

    private static final String[] HIGH_STRESS_SUFFIXES = {
        " (Je me sens très mal tout à coup.)",
        " (Est-ce qu'il fait chaud ici ou c'est moi ?)",
        " (Je regrette d'être venu témoigner.)",
        " (Mon cœur bat à 200 là.)",
    };
    private static final String[] MED_STRESS_SUFFIXES = {
        " Je suis un peu nerveux, c'est tout.",
        " Désolé si je manque de clarté.",
        " Ces interrogatoires me perturbent.",
    };

    private static final String[] CONTRADICTION_SUFFIXES = {
        " Ce témoin se contredit allègrement.",
        " Le juré n°4 lève un sourcil suspicieux.",
        " Un murmure parcourt la salle d'audience.",
        " Quelqu'un tousse de façon suspecte dans le fond.",
        " Le greffier s'arrête d'écrire pour regarder.",
    };

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
                    "Le témoin %s contredit la preuve \"%s\".%s",
                    witness.getName(), evidence.getDescription(),
                    CONTRADICTION_SUFFIXES[random.nextInt(CONTRADICTION_SUFFIXES.length)]
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
                        "Contradiction entre %s et %s sur le sujet : %s — au moins l'un des deux ment.",
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
        return generateContextualAnswer(witness, question);
    }

    private String generateInconsistentAnswer(Witness witness) {
        Statement initial = witness.getInitialStatement();
        String base = (initial != null) ? initial.getContent() : "Je ne me souviens plus très bien...";
        String template = STRESSED_RESPONSES[random.nextInt(STRESSED_RESPONSES.length)];
        return String.format(template, base);
    }

    /**
     * Génère une réponse contextuelle selon le type de question posée
     * et la personnalité du témoin.
     */
    private String generateContextualAnswer(Witness witness, String question) {
        String q = (question == null) ? "" : question.toLowerCase();
        String stmt = (witness.getInitialStatement() != null)
            ? witness.getInitialStatement().getContent()
            : "je n'ai rien de particulier à déclarer";

        Witness.WitnessPersonality p = (witness.getPersonality() != null)
            ? witness.getPersonality()
            : Witness.WitnessPersonality.COOPERATIVE;

        int pIdx = p.ordinal();
        // Toujours variante 1 ou 2 : la déclaration initiale est déjà visible sur la carte témoin
        int variant = 1 + (Math.abs(question != null ? question.hashCode() : 0) % 2);

        String response;
        if (containsAny(q, "alibi", "où étiez", "faisiez-vous", "trouviez-vous", "étiez-vous", "présent")) {
            response = pickResponse(ALIBI_RESPONSES, pIdx, variant, stmt);
        } else if (containsAny(q, "mentez", "mentir", "mensonge", "faux", "inventer", "cacher", "cache", "vrai", "témoignage", "influencé", "déposition")) {
            response = pickResponse(CHALLENGE_RESPONSES, pIdx, variant, stmt);
        } else if (containsAny(q, "connaissiez", "connaissez", "relation", "lien", "fréquentiez", "rapport", "ami")) {
            response = pickResponse(RELATIONSHIP_RESPONSES, pIdx, variant, stmt);
        } else if (containsAny(q, "vu", "observé", "remarqué", "aperçu", "entendu", "cri", "bruit", "voir")) {
            response = pickResponse(OBSERVATION_RESPONSES, pIdx, variant, stmt);
        } else if (containsAny(q, "heure", "quand", "moment", "date", "temps", "soir", "nuit", "matin")) {
            response = pickResponse(TIMELINE_RESPONSES, pIdx, variant, stmt);
        } else if (containsAny(q, "pourquoi", "raison", "motif", "intérêt", "bénéfice", "mobile")) {
            response = pickResponse(MOTIVE_RESPONSES, pIdx, variant, stmt);
        } else {
            response = pickResponse(GENERIC_RESPONSES, pIdx, variant, stmt);
        }

        return addStressSuffix(response, witness.getStressLevel());
    }

    private String pickResponse(String[][] pool, int pIdx, int variant, String stmt) {
        String[] variants = pool[pIdx % pool.length];
        String template   = variants[variant % variants.length];
        return template.contains("%s") ? String.format(template, stmt) : template;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String addStressSuffix(String response, double stress) {
        if (stress > 0.75) {
            return response + HIGH_STRESS_SUFFIXES[random.nextInt(HIGH_STRESS_SUFFIXES.length)];
        } else if (stress > 0.45) {
            return response + MED_STRESS_SUFFIXES[random.nextInt(MED_STRESS_SUFFIXES.length)];
        }
        return response;
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
                c.setDescription("Le témoin vient de se contredire lui-même."
                    + CONTRADICTION_SUFFIXES[random.nextInt(CONTRADICTION_SUFFIXES.length)]);
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
