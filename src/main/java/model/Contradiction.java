package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Une contradiction détectée entre deux déclarations d'un même témoin.
 * Le joueur doit l'identifier et choisir le bon moment pour la soulever.
 * La soulever au bon moment augmente son score et fait baisser
 * la crédibilité du témoin aux yeux du jury.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Contradiction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Première déclaration impliquée dans la contradiction.
     */
    @ManyToOne
    @JoinColumn(name = "statement1_id")
    private Statement statement1;

    /**
     * Deuxième déclaration qui contredit la première.
     */
    @ManyToOne
    @JoinColumn(name = "statement2_id")
    private Statement statement2;

    /**
     * Explication de la contradiction (visible une fois détectée).
     * ex: "Le témoin dit être chez lui à 20h, mais a déclaré plus tôt être au bar."
     */
    @Column(length = 1000)
    private String description;

    /**
     * Indique si le joueur a détecté et soulevé cette contradiction.
     */
    private boolean detected = false;
}
