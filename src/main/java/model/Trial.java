package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * La session de jeu — une partie en cours.
 * Relie le joueur (Lawyer) à une affaire (Case) et suit l'état du procès :
 * les déclarations accumulées, les contradictions détectées et le tour actuel.
 * Le statut passe à GUILTY ou NOT_GUILTY quand le jury délibère.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Trial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * L'affaire sur laquelle porte ce procès.
     */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "case_id")
    private Case lawCase;

    /**
     * Le joueur — avocat de la défense ou procureur.
     */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "lawyer_id")
    private Lawyer lawyer;

    /**
     * Les membres du jury de ce procès.
     */
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "trial_id")
    private List<JuryMember> jury;

    /**
     * Toutes les déclarations faites pendant ce procès.
     */
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "trial_id")
    private List<Statement> statements;

    /**
     * Contradictions détectées et soulevées par le joueur.
     */
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "trial_id")
    private List<Contradiction> detectedContradictions;

    /**
     * Tour de jeu actuel (chaque question posée = +1 tour).
     */
    private int currentTurn = 0;

    /**
     * État du procès.
     * IN_PROGRESS : le joueur joue encore
     * GUILTY      : le jury a déclaré le suspect coupable
     * NOT_GUILTY  : le jury a déclaré le suspect non coupable
     */
    @Enumerated(EnumType.STRING)
    private TrialStatus status = TrialStatus.IN_PROGRESS;

    /**
     * Moment où la partie a démarré.
     */
    private LocalDateTime startedAt = LocalDateTime.now();

    public enum TrialStatus {
        IN_PROGRESS,
        GUILTY,
        NOT_GUILTY
    }
}
