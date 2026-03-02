package service;

import model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CaseGeneratorService {

    private static final Random random = new Random();

    private static final List<CaseTemplate> CASE_TEMPLATES = List.of(
        new CaseTemplate(
            "Le Meurtre au Manoir",
            "Loris Lebelge est accusé du meurtre de son associé Ugo, retrouvé mort dans son bureau.",
            Case.CrimeType.MURDER,
            List.of(
                new WitnessTemplate("Marie Dupont", "Secrétaire", "J'ai entendu des cris vers 22h, puis plus rien.", true),
                new WitnessTemplate("Paul Bernard", "Voisin", "J'ai vu une silhouette quitter le bâtiment en courant.", false)
            ),
            List.of(
                new EvidenceTemplate("Couteau ensanglanté", 0.85, true),
                new EvidenceTemplate("Relevé téléphonique", 0.60, true),
                new EvidenceTemplate("Alibi non vérifié", 0.40, false)
            )
        ),
        new CaseTemplate(
            "L'Escroquerie Financière",
            "Sophie Martin est accusée d'avoir détourné 500 000€ de son entreprise.",
            Case.CrimeType.FRAUD,
            List.of(
                new WitnessTemplate("Jean Lefèvre", "Comptable", "Les comptes ne correspondent pas depuis 6 mois.", true),
                new WitnessTemplate("Claire Morin", "Collègue", "Elle semblait très stressée ces derniers temps.", false)
            ),
            List.of(
                new EvidenceTemplate("Virement bancaire suspect", 0.90, true),
                new EvidenceTemplate("Email compromettant", 0.70, true),
                new EvidenceTemplate("Témoignage de caractère", 0.30, false)
            )
        ),
        new CaseTemplate(
            "Le Vol au Musée",
            "Thomas Petit est accusé du vol d'un tableau estimé à 1 million d'euros.",
            Case.CrimeType.THEFT,
            List.of(
                new WitnessTemplate("Isabelle Roux", "Gardienne", "Il était présent lors de la fermeture, c'est inhabituel.", true),
                new WitnessTemplate("Marc Blanc", "Restaurateur", "Je l'ai vu examiner le tableau de près plusieurs fois.", false)
            ),
            List.of(
                new EvidenceTemplate("Enregistrement caméra", 0.80, true),
                new EvidenceTemplate("Empreintes digitales", 0.75, true),
                new EvidenceTemplate("Reçu d'achat d'outils", 0.50, false)
            )
        )
    );

    public Case generateRandomCase() {
        CaseTemplate template = CASE_TEMPLATES.get(random.nextInt(CASE_TEMPLATES.size()));
        return buildCaseFromTemplate(template);
    }

    public Case generateCaseByType(Case.CrimeType crimeType) {
        return CASE_TEMPLATES.stream()
            .filter(t -> t.crimeType == crimeType)
            .findFirst()
            .map(this::buildCaseFromTemplate)
            .orElse(generateRandomCase());
    }

    private Case buildCaseFromTemplate(CaseTemplate template) {
        Case cas = new Case();
        cas.setTitle(template.title);
        cas.setDescription(template.description);
        cas.setCrimeType(template.crimeType);

        Suspect suspect = new Suspect();
        suspect.setName("Suspect de l'affaire : " + template.title);
        double guiltProb = 0.5 + (random.nextDouble() * 0.4);
        suspect.setGuiltProbability(guiltProb);
        suspect.setGuilty(guiltProb > 0.65);
        cas.setSuspect(suspect);

        List<Witness> witnesses = new ArrayList<>();
        for (WitnessTemplate wt : template.witnesses) {
            Witness w = new Witness();
            w.setName(wt.name);
            w.setProfession(wt.profession);
            double reliability = wt.isReliable
                ? 0.7 + random.nextDouble() * 0.3
                : 0.2 + random.nextDouble() * 0.3;
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
            member.setConvictionLevel(0.5);
            jury.add(member);
        }
        cas.setJury(jury);

        return cas;
    }

    private record CaseTemplate(
        String title,
        String description,
        Case.CrimeType crimeType,
        List<WitnessTemplate> witnesses,
        List<EvidenceTemplate> evidences
    ) {}

    private record WitnessTemplate(String name, String profession, String statement, boolean isReliable) {}

    private record EvidenceTemplate(String description, double weight, boolean isAuthentic) {}
}
