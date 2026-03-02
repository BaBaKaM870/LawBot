package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Un témoin dans l'affaire.
 * A une personnalité qui influence ses réponses et un niveau de crédibilité
 * qui évolue pendant le procès.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Witness extends Person {

    /**
     * Personnalité du témoin — influence ses réponses et sa tendance à mentir.
     * NERVOUS   : peut se contredire sous pression
     * LIAR      : ment délibérément, contradictions détectables
     * CONFIDENT : cohérent, difficile à déstabiliser
     * COOPERATIVE : répond franchement, peu de contradictions
     */
    @Enumerated(EnumType.STRING)
    private WitnessPersonality personality;

    /**
     * Crédibilité du témoin aux yeux du jury (0 à 100).
     * Baisse quand une contradiction est détectée, monte si ses réponses sont cohérentes.
     */
    private int credibility = 50;

    /**
     * Toutes les déclarations faites par ce témoin pendant le procès.
     */
    @OneToMany(mappedBy = "witness", cascade = CascadeType.ALL)
    private List<Statement> statements;

    /**
     * Indique si ce témoin est actuellement à la barre.
     */
    private boolean onStand = false;

    public enum WitnessPersonality {
        NERVOUS,
        LIAR,
        CONFIDENT,
        COOPERATIVE
    }
}
