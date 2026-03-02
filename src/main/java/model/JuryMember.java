package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Un membre du jury.
 * Chaque juré a un profil psychologique et un niveau de conviction
 * qui évolue selon les actions du joueur pendant le procès.
 * Le verdict final est calculé à partir de la moyenne des convictions.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class JuryMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /**
     * Profil psychologique du juré — détermine ce qui le convainc.
     * SKEPTICAL  : difficile à convaincre, exige des preuves solides
     * EMOTIONAL  : sensible aux témoignages, peu aux faits techniques
     * LOGICAL    : se fie aux faits et aux preuves, ignore les émotions
     * BIASED     : a une opinion préconçue (pro-défense ou pro-accusation)
     */
    @Enumerated(EnumType.STRING)
    private JurorProfile profile;

    /**
     * Niveau de conviction que le suspect est coupable (0 à 100).
     * En dessous de 50 → penche pour l'innocence.
     * Au dessus de 50  → penche pour la culpabilité.
     * Évolue à chaque preuve présentée ou contradiction détectée.
     */
    private int convictionLevel = 50;

    public enum JurorProfile {
        SKEPTICAL,
        EMOTIONAL,
        LOGICAL,
        BIASED
    }
}
