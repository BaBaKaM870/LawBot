package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Une preuve dans l'affaire.
 * Chaque preuve a un poids sur le jury et une authenticité cachée au joueur.
 * Le joueur doit choisir lesquelles présenter — une fausse preuve peut retourner le jury.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // ex: "Couteau ensanglanté", "Relevé téléphonique"
    private String description; // Détails narratifs visibles par le joueur

    /**
     * Catégorie de la preuve.
     * PHYSICAL    : objet concret (arme, empreinte, ADN)
     * DOCUMENTARY : document écrit (contrat, email, reçu)
     * TESTIMONIAL : déclaration formelle d'un témoin
     * DIGITAL     : enregistrement, vidéo, données informatiques
     * FORENSIC    : rapport d'expert (médecin légiste, expert balistique)
     */
    @Enumerated(EnumType.STRING)
    private EvidenceType type;

    /** Poids de la preuve sur la conviction du jury (0.0 à 1.0). */
    private double weight;

    private boolean authentic;

    /** Indique si le joueur a contesté cette preuve. */
    private boolean contested = false;

    private boolean revealed = false;

    public enum EvidenceType {
        PHYSICAL,
        DOCUMENTARY,
        TESTIMONIAL,
        DIGITAL,
        FORENSIC
    }
}
