package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Le suspect de l'affaire — peut être coupable ou innocent.
 * Sa culpabilité réelle est cachée au joueur et sert à générer
 * les incohérences, les preuves et les déclarations des témoins.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Suspect extends Person {

    /**
     * Alibi déclaré par le suspect.
     * Peut être vérifié ou contredit par les preuves et témoins.
     */
    private String alibi;

    /**
     * Mobile du suspect (ex: "jalousie", "argent", "vengeance").
     * Utilisé lors de la génération procédurale de l'affaire.
     */
    private String motive;

    /**
     * Culpabilité réelle — cachée au joueur.
     * Déterminée lors de la génération de l'affaire.
     */
    private boolean guilty;

    /** Probabilité de culpabilité (0.0 à 1.0) — déterminée à la génération. */
    private double guiltProbability = 0.5;
}
