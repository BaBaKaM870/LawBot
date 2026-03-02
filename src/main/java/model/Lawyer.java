package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * L'avocat — c'est le joueur.
 * Peut incarner la défense ou l'accusation.
 * Son score évolue selon la qualité de ses actions pendant le procès.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Lawyer extends Person {

    /**
     * Camp du joueur : DEFENSE ou PROSECUTION.
     */
    @Enumerated(EnumType.STRING)
    private Side side;

    /**
     * Score du joueur sur la partie en cours (0 à 100).
     * Augmente en détectant des contradictions, présentant de bonnes preuves, etc.
     */
    private int score = 0;

    public enum Side {
        DEFENSE,
        PROSECUTION
    }
}
