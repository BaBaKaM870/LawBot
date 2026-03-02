package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Une déclaration faite par un témoin lors du procès.
 * Chaque question posée génère un Statement.
 * Les contradictions sont détectées en comparant deux Statements.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Le témoin qui a fait cette déclaration.
     */
    @ManyToOne
    @JoinColumn(name = "witness_id")
    private Witness witness;

    /**
     * La question posée par le joueur.
     */
    private String question;

    /**
     * La réponse du témoin (générée selon sa personnalité et la réalité de l'affaire).
     */
    @Column(length = 1000)
    private String content;

    /**
     * Indique si cette déclaration est un mensonge.
     * Caché au joueur — utilisé pour détecter les contradictions.
     */
    private boolean lie;

    /**
     * Tour de jeu auquel cette déclaration a été faite.
     * Permet de retrouver l'ordre des déclarations dans le procès.
     */
    private int turn;
}
