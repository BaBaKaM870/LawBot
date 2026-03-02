package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Witness extends Person {

    @Enumerated(EnumType.STRING)
    private WitnessPersonality personality;

    private int credibility = 50;

    private String profession;

    /** Fiabilité du témoin (0.0 à 1.0) — utilisée pour calculer la crédibilité des réponses. */
    private double reliability = 0.7;

    /** Niveau de stress accumulé pendant l'interrogatoire (0.0 à 1.0). */
    private double stressLevel = 0.0;

    /** Déclaration initiale du témoin — donnée avant toute question du joueur. */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "initial_statement_id")
    private Statement initialStatement;

    /** Toutes les déclarations faites par ce témoin pendant le procès. */
    @OneToMany(mappedBy = "witness", cascade = CascadeType.ALL)
    private List<Statement> statements = new ArrayList<>();

    /** Contradictions détectées dans les déclarations de ce témoin. */
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "witness_contradiction_id")
    private List<Contradiction> contradictions = new ArrayList<>();

    private boolean onStand = false;

    public void addStatement(Statement stmt) {
        if (this.statements == null) this.statements = new ArrayList<>();
        this.statements.add(stmt);
    }

    public void addContradiction(Contradiction c) {
        if (this.contradictions == null) this.contradictions = new ArrayList<>();
        this.contradictions.add(c);
    }

    public void increaseStress(double amount) {
        this.stressLevel = Math.min(1.0, this.stressLevel + amount);
    }

    public enum WitnessPersonality {
        NERVOUS,
        LIAR,
        CONFIDENT,
        COOPERATIVE
    }
}
