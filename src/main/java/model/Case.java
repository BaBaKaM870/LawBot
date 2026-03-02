package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

/**
 * L'affaire complète — générée procéduralement au début de chaque partie.
 * Contient tout le contexte narratif : le suspect, les témoins, les preuves
 * et la description du crime.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "law_case") // "case" est un mot réservé SQL
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;       // ex: "L'affaire Martin"
    private String crimeDescription; // Résumé du crime présenté au joueur

    /**
     * Type de crime — utilisé pour orienter la génération procédurale.
     */
    @Enumerated(EnumType.STRING)
    private CrimeType crimeType;

    /**
     * Date à laquelle le crime a été commis (dans la fiction).
     */
    private LocalDate crimeDate;

    /**
     * Le suspect principal de l'affaire.
     */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "suspect_id")
    private Suspect suspect;

    /**
     * Les témoins disponibles à interroger.
     */
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "case_id")
    private List<Witness> witnesses;

    /**
     * Les preuves disponibles dans l'affaire.
     * Certaines soutiennent la culpabilité, d'autres l'innocence.
     */
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "case_id")
    private List<Evidence> evidences;

    public enum CrimeType {
        MURDER,
        THEFT,
        FRAUD,
        ASSAULT,
        CORRUPTION
    }
}
