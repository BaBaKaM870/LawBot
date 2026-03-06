package service;

import model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CaseGeneratorService {

    private static final Random random = new Random();

    private static final List<CaseTemplate> CASE_TEMPLATES = List.of(

        // ── MEURTRES ──────────────────────────────────────────────────────────

        new CaseTemplate(
            "Le Meurtre au Manoir",
            "Loris Lebelge est accusé du meurtre de son associé Ugo Ferri, " +
            "retrouvé mort dans son bureau avec une blessure par arme blanche.",
            "Loris Lebelge",
            Case.CrimeType.MURDER,
            List.of(
                new WitnessTemplate("Marie Dupont", "Secrétaire",
                    "J'ai entendu des cris vers 22h, puis plus rien. M. Lebelge est parti très agité.", true),
                new WitnessTemplate("Paul Bernard", "Voisin",
                    "J'ai vu une silhouette quitter le bâtiment en courant. Je n'ai pas bien vu le visage.", false)
            ),
            List.of(
                new EvidenceTemplate("Couteau ensanglanté portant des empreintes", 0.85, true),
                new EvidenceTemplate("Relevé téléphonique : appel manqué à 21h50", 0.60, true),
                new EvidenceTemplate("Alibi invérifiable : cinéma seul", 0.40, false)
            )
        ),

        new CaseTemplate(
            "Le Meurtre à l'Hôtel des Arts",
            "Camille Beaumont est accusée d'avoir empoisonné son mari René, " +
            "producteur de cinéma, retrouvé mort dans leur suite d'hôtel parisienne.",
            "Camille Beaumont",
            Case.CrimeType.MURDER,
            List.of(
                new WitnessTemplate("Hugo Petit", "Serveur de l'hôtel",
                    "J'ai vu Mme Beaumont commander une bouteille de vin et retourner seule dans la suite vers 23h.", true),
                new WitnessTemplate("Diane Lefèvre", "Amie du couple",
                    "René et Camille s'entendaient parfaitement, c'était un couple modèle. Aucun problème.", false),
                new WitnessTemplate("Pr. Laurent Girard", "Toxicologue",
                    "Les analyses révèlent une dose mortelle d'arsenic dans le sang de la victime.", true)
            ),
            List.of(
                new EvidenceTemplate("Flacon d'arsenic retrouvé dans le sac de l'accusée", 0.88, true),
                new EvidenceTemplate("Assurance-vie de 2 millions € au bénéfice de l'accusée", 0.65, true),
                new EvidenceTemplate("Reçu de pharmacie dont la date a été modifiée", 0.70, false),
                new EvidenceTemplate("Témoignage de l'amie du couple (partiale)", 0.30, false)
            )
        ),

        new CaseTemplate(
            "L'Incendie Criminel",
            "Patricia Vidal est accusée d'avoir volontairement incendié l'entrepôt " +
            "de son ex-associé, causant la mort du gardien de nuit Karim Azizi.",
            "Patricia Vidal",
            Case.CrimeType.MURDER,
            List.of(
                new WitnessTemplate("Stéphane Ortiz", "Pompier",
                    "L'incendie s'est propagé trop vite pour être accidentel. Des traces d'accélérant sont visibles.", true),
                new WitnessTemplate("Rosie Nakamura", "Voisine",
                    "J'ai cru voir une voiture bleue stationner devant l'entrepôt vers 2h, mais je n'en suis pas sûre.", false),
                new WitnessTemplate("Daniel Ferreira", "Ex-associé",
                    "Patricia m'avait menacé de tout faire brûler si je ne la remboursais pas. C'est dans ses textos.", true)
            ),
            List.of(
                new EvidenceTemplate("Traces d'accélérant à l'entrée de l'entrepôt", 0.88, true),
                new EvidenceTemplate("Ticket d'essence à 2h15 — station à 800m du lieu", 0.72, true),
                new EvidenceTemplate("Voiture bleue similaire aperçue (non identifiée)", 0.45, false),
                new EvidenceTemplate("SMS de menaces envoyés depuis son téléphone", 0.82, true)
            )
        ),

        new CaseTemplate(
            "Le Meurtre du Journaliste",
            "Grégoire Villeneuve, directeur de cabinet, est accusé d'avoir commandité " +
            "l'assassinat du journaliste Alain Forêt, retrouvé abattu dans sa voiture.",
            "Grégoire Villeneuve",
            Case.CrimeType.MURDER,
            List.of(
                new WitnessTemplate("Sophie Renard", "Journaliste collègue",
                    "Alain m'avait confié qu'il craignait pour sa vie. Il avait reçu des menaces anonymes.", true),
                new WitnessTemplate("Commissaire Leroy", "Police judiciaire",
                    "L'arme retrouvée appartient à un individu lié au cabinet du directeur.", true),
                new WitnessTemplate("Xavier Perrault", "Chauffeur du cabinet",
                    "M. Villeneuve était en réunion toute la nuit. Je l'attendais dehors.", false)
            ),
            List.of(
                new EvidenceTemplate("Arme retrouvée — traçable jusqu'au cabinet", 0.85, true),
                new EvidenceTemplate("SMS de menaces depuis un téléphone prépayé lié à l'accusé", 0.70, true),
                new EvidenceTemplate("Alibi du chauffeur (témoignage potentiellement fabriqué)", 0.40, false),
                new EvidenceTemplate("Virement vers une organisation douteuse", 0.78, true)
            )
        ),

        // ── FRAUDES ───────────────────────────────────────────────────────────

        new CaseTemplate(
            "L'Escroquerie Financière",
            "Sophie Martin est accusée d'avoir détourné 500 000 € des comptes " +
            "de son entreprise au profit de sociétés écrans qu'elle contrôle.",
            "Sophie Martin",
            Case.CrimeType.FRAUD,
            List.of(
                new WitnessTemplate("Jean Lefèvre", "Comptable",
                    "Les comptes ne correspondent pas depuis 6 mois. Des sommes partent vers des sociétés inconnues.", true),
                new WitnessTemplate("Claire Morin", "Collègue de bureau",
                    "Sophie semblait très stressée ces derniers temps et évitait les réunions financières.", false)
            ),
            List.of(
                new EvidenceTemplate("Virement bancaire suspect vers une société offshore", 0.90, true),
                new EvidenceTemplate("Email interne compromettant signé de l'accusée", 0.72, true),
                new EvidenceTemplate("Témoignage de caractère d'une amie (partiale)", 0.28, false)
            )
        ),

        new CaseTemplate(
            "La Fraude à l'Héritage",
            "Julien Moreau est accusé d'avoir falsifié le testament de sa grand-mère " +
            "pour s'approprier un patrimoine de 3 millions d'euros.",
            "Julien Moreau",
            Case.CrimeType.FRAUD,
            List.of(
                new WitnessTemplate("Maître Cécile Dupré", "Notaire",
                    "Le testament présenté présente des irrégularités graphologiques manifestes.", true),
                new WitnessTemplate("Bernard Moreau", "Frère de l'accusé",
                    "Julien avait des dettes importantes. Il m'avait dit qu'il trouverait une solution.", true),
                new WitnessTemplate("Madeleine Tissot", "Voisine de la grand-mère",
                    "La vieille dame me disait toujours que Julien était son petit-fils préféré.", false)
            ),
            List.of(
                new EvidenceTemplate("Testament contesté avec signature irrégulière", 0.82, true),
                new EvidenceTemplate("Expertise graphologique confirmant la falsification", 0.78, true),
                new EvidenceTemplate("Factures d'un graphiste freelance (non prouvée)", 0.60, false),
                new EvidenceTemplate("Relevé bancaire montrant des dettes importantes", 0.45, false)
            )
        ),

        // ── VOLS ──────────────────────────────────────────────────────────────

        new CaseTemplate(
            "Le Vol au Musée",
            "Thomas Petit est accusé du vol d'un tableau de Monet estimé " +
            "à 1 million d'euros au musée des Beaux-Arts.",
            "Thomas Petit",
            Case.CrimeType.THEFT,
            List.of(
                new WitnessTemplate("Isabelle Roux", "Gardienne",
                    "Il était présent lors de la fermeture, ce qui est inhabituel pour un simple visiteur.", true),
                new WitnessTemplate("Marc Blanc", "Restaurateur d'art",
                    "Je l'ai vu examiner le tableau de très près et photographier le cadre plusieurs fois.", false)
            ),
            List.of(
                new EvidenceTemplate("Enregistrement caméra — silhouette identifiable", 0.80, true),
                new EvidenceTemplate("Empreintes digitales sur le cadre vide", 0.75, true),
                new EvidenceTemplate("Reçu d'achat d'outils spécialisés en ligne", 0.50, false)
            )
        ),

        new CaseTemplate(
            "Le Braquage de la Bijouterie",
            "Lucas Faure est accusé d'avoir braqué la bijouterie Delacroix " +
            "avec deux complices, emportant pour 800 000 € de bijoux.",
            "Lucas Faure",
            Case.CrimeType.THEFT,
            List.of(
                new WitnessTemplate("Valérie Delacroix", "Propriétaire",
                    "L'un des braqueurs avait un tatouage de serpent sur l'avant-bras gauche, je l'ai bien vu.", true),
                new WitnessTemplate("Kevin Simons", "Passant",
                    "J'ai vu trois types sortir en courant. Deux avaient des casques de moto.", false),
                new WitnessTemplate("Inspecteur Hakimi", "Police technique",
                    "L'analyse des empreintes sur le coffre correspond à celles de l'accusé dans notre fichier.", true)
            ),
            List.of(
                new EvidenceTemplate("Empreintes digitales sur le coffre forcé", 0.85, true),
                new EvidenceTemplate("Caméra montrant un tatouage de serpent", 0.72, true),
                new EvidenceTemplate("Moto retrouvée à 2km — non prouvée comme étant la sienne", 0.52, false),
                new EvidenceTemplate("Billet de 500 € tracé retrouvé chez l'accusé", 0.78, true)
            )
        ),

        new CaseTemplate(
            "Le Cambriolage Nocturne",
            "Arnaud Tissier est accusé d'avoir cambriolé la villa du baron Garnier " +
            "pendant ses vacances, emportant bijoux et œuvres d'art pour 400 000 €.",
            "Arnaud Tissier",
            Case.CrimeType.THEFT,
            List.of(
                new WitnessTemplate("Monique Léger", "Gardienne du quartier",
                    "J'ai vu une fourgonnette blanche s'arrêter devant la villa à 23h, mais je n'étais pas près.", false),
                new WitnessTemplate("Thierry Garnier", "Fils de la victime",
                    "Arnaud était notre jardinier depuis 2 ans — il connaissait parfaitement le code de l'alarme.", true),
                new WitnessTemplate("Serge Blondel", "Receleur présumé",
                    "Je n'ai rien acheté d'illégal. Tout vient de ventes aux enchères parfaitement légales.", false)
            ),
            List.of(
                new EvidenceTemplate("Code alarme désactivé avec le code personnel du jardinier", 0.82, true),
                new EvidenceTemplate("Collier de la comtesse retrouvé chez le receleur", 0.75, true),
                new EvidenceTemplate("Fourgonnette aperçue — plaque non lisible", 0.50, false),
                new EvidenceTemplate("Outil de crochetage retrouvé dans le véhicule de l'accusé", 0.62, false)
            )
        ),

        // ── AGRESSIONS ────────────────────────────────────────────────────────

        new CaseTemplate(
            "L'Agression dans le Parc",
            "Antoine Renard est accusé d'avoir violemment agressé Marco Ibáñez " +
            "dans le parc des Buttes-Chaumont. La victime est hospitalisée.",
            "Antoine Renard",
            Case.CrimeType.ASSAULT,
            List.of(
                new WitnessTemplate("Fatima Ouali", "Joggeuse",
                    "J'ai clairement vu l'agresseur frapper l'homme à plusieurs reprises. Il portait une veste rouge.", true),
                new WitnessTemplate("Roger Kastner", "Retraité",
                    "J'entends des gens se disputer souvent dans ce parc. Je n'ai pas bien vu cette fois-là.", false),
                new WitnessTemplate("Dr. Nadia Chen", "Médecin urgentiste",
                    "Les blessures sont compatibles avec des coups répétés. Fractures multiples.", true)
            ),
            List.of(
                new EvidenceTemplate("Vidéo de surveillance montrant une veste rouge", 0.65, true),
                new EvidenceTemplate("Témoignage oculaire direct de la joggeuse", 0.75, true),
                new EvidenceTemplate("Traces d'ADN sous les ongles de la victime", 0.88, true),
                new EvidenceTemplate("Veste rouge retrouvée — non certifiée être la sienne", 0.50, false)
            )
        ),

        new CaseTemplate(
            "La Rixe au Bar",
            "Nicolas Durand est accusé d'avoir grièvement blessé un client " +
            "lors d'une rixe au bar Le Refuge, utilisant une bouteille brisée.",
            "Nicolas Durand",
            Case.CrimeType.ASSAULT,
            List.of(
                new WitnessTemplate("Laura Petit", "Barmaid",
                    "J'ai tout vu : c'est Durand qui a pris la bouteille en premier et qui a frappé sans raison.", true),
                new WitnessTemplate("Rémi Courtois", "Ami de l'accusé",
                    "Nicolas s'est défendu, c'est l'autre qui a commencé à l'insulter violemment.", false),
                new WitnessTemplate("Dr. Paul Vernet", "Médecin légiste",
                    "La blessure correspond à un impact de verre. La cicatrice est profonde, ce n'était pas accidentel.", true)
            ),
            List.of(
                new EvidenceTemplate("Caméra du bar : Durand saisit la bouteille en premier", 0.88, true),
                new EvidenceTemplate("Rapport médical détaillant la gravité des blessures", 0.70, true),
                new EvidenceTemplate("Témoignage de l'ami — clairement partial", 0.25, false),
                new EvidenceTemplate("Verre brisé avec empreintes partielles", 0.58, false)
            )
        ),

        // ── CORRUPTION ────────────────────────────────────────────────────────

        new CaseTemplate(
            "La Corruption Municipale",
            "Édouard Marchetti, conseiller municipal, est accusé d'avoir accepté " +
            "des pots-de-vin de promoteurs pour l'octroi de permis de construire illégaux.",
            "Édouard Marchetti",
            Case.CrimeType.CORRUPTION,
            List.of(
                new WitnessTemplate("Sandra Leroy", "Comptable de la mairie",
                    "Des virements inhabituels ont transité par un compte offshore lié à M. Marchetti.", true),
                new WitnessTemplate("Pierre Duval", "Promoteur immobilier",
                    "Je n'ai jamais versé d'argent à qui que ce soit. C'est une calomnie politique.", false),
                new WitnessTemplate("Alice Fontaine", "Journaliste d'investigation",
                    "J'ai des enregistrements de leurs conversations où les conditions du deal sont évoquées.", true)
            ),
            List.of(
                new EvidenceTemplate("Enregistrement audio partiellement effacé", 0.62, false),
                new EvidenceTemplate("Virements sur compte offshore — 120 000 €", 0.92, true),
                new EvidenceTemplate("Permis de construire accordés hors procédure normale", 0.80, true),
                new EvidenceTemplate("Témoignage du promoteur — suspect et contradictoire", 0.30, false)
            )
        ),

        new CaseTemplate(
            "L'Espionnage Industriel",
            "Victor Sanz, ingénieur en chef, est accusé d'avoir vendu les plans secrets " +
            "d'un nouveau processeur à une entreprise concurrente pour 1,2 million d'euros.",
            "Victor Sanz",
            Case.CrimeType.CORRUPTION,
            List.of(
                new WitnessTemplate("Dr. Isabelle Morin", "Directrice R&D",
                    "Seul Victor avait accès aux fichiers ultra-confidentiels. Son badge a été utilisé à 3h du matin.", true),
                new WitnessTemplate("Frank Mueller", "Collègue ingénieur",
                    "Victor m'a montré une liasse de billets de 500 € la semaine dernière, sans explication.", true),
                new WitnessTemplate("Mia Chen", "DRH de l'entreprise",
                    "Victor est un employé modèle depuis 8 ans. Je ne peux pas y croire.", false)
            ),
            List.of(
                new EvidenceTemplate("Fichiers confidentiels récupérés sur serveur étranger", 0.92, true),
                new EvidenceTemplate("Virement reçu depuis Singapour : 1 200 000 €", 0.88, true),
                new EvidenceTemplate("Clé USB chiffrée retrouvée dans son bureau", 0.68, false),
                new EvidenceTemplate("Photo floue d'une réunion dans un café", 0.38, false)
            )
        )

    );

    // ════════════════════════════════════════════════════════════════════════
    //  API publique
    // ════════════════════════════════════════════════════════════════════════

    public Case generateRandomCase() {
        CaseTemplate template = CASE_TEMPLATES.get(random.nextInt(CASE_TEMPLATES.size()));
        return buildCaseFromTemplate(template);
    }

    public Case generateCaseByType(Case.CrimeType crimeType) {
        List<CaseTemplate> filtered = CASE_TEMPLATES.stream()
            .filter(t -> t.crimeType == crimeType)
            .toList();
        CaseTemplate template = filtered.isEmpty()
            ? CASE_TEMPLATES.get(random.nextInt(CASE_TEMPLATES.size()))
            : filtered.get(random.nextInt(filtered.size()));
        return buildCaseFromTemplate(template);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Construction de l'affaire
    // ════════════════════════════════════════════════════════════════════════

    private Case buildCaseFromTemplate(CaseTemplate template) {
        Case cas = new Case();
        cas.setTitle(template.title);
        cas.setDescription(template.description);
        cas.setCrimeType(template.crimeType);

        Suspect suspect = new Suspect();
        suspect.setName(template.suspectName);
        double guiltProb = 0.45 + (random.nextDouble() * 0.45);
        suspect.setGuiltProbability(guiltProb);
        suspect.setGuilty(guiltProb > 0.62);
        cas.setSuspect(suspect);

        List<Witness> witnesses = new ArrayList<>();
        for (WitnessTemplate wt : template.witnesses) {
            Witness w = new Witness();
            w.setName(wt.name);
            w.setProfession(wt.profession);
            double reliability = wt.isReliable
                ? 0.65 + random.nextDouble() * 0.35
                : 0.15 + random.nextDouble() * 0.35;
            w.setReliability(reliability);
            Statement stmt = new Statement();
            stmt.setContent(wt.statement);
            stmt.setWitnessName(wt.name);
            w.setInitialStatement(stmt);
            witnesses.add(w);
        }
        cas.setWitnesses(witnesses);

        List<Evidence> evidences = new ArrayList<>();
        for (EvidenceTemplate et : template.evidences) {
            Evidence e = new Evidence();
            e.setDescription(et.description);
            e.setWeight(et.weight);
            e.setAuthentic(et.isAuthentic);
            evidences.add(e);
        }
        cas.setEvidences(evidences);

        List<JuryMember> jury = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            JuryMember member = new JuryMember();
            member.setName("Juré n°" + i);
            member.setConvictionLevel(0.5 + (random.nextDouble() - 0.5) * 0.2);
            jury.add(member);
        }
        cas.setJury(jury);

        return cas;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Records internes
    // ════════════════════════════════════════════════════════════════════════

    private record CaseTemplate(
        String title,
        String description,
        String suspectName,
        Case.CrimeType crimeType,
        List<WitnessTemplate> witnesses,
        List<EvidenceTemplate> evidences
    ) {}

    private record WitnessTemplate(
        String name, String profession, String statement, boolean isReliable
    ) {}

    private record EvidenceTemplate(
        String description, double weight, boolean isAuthentic
    ) {}
}
