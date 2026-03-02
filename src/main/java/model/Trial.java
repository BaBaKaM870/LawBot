package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Trial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "case_id")
    private Case lawCase;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "lawyer_id")
    private Lawyer lawyer;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "trial_id")
    private List<Statement> statements;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "trial_id")
    private List<Contradiction> detectedContradictions;

    @Enumerated(EnumType.STRING)
    private TrialStatus status = TrialStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    private TrialPhase currentPhase = TrialPhase.OPENING_STATEMENTS;

    private boolean active = true;

    private int currentTurn = 0;

    private int successfulActionsCount = 0;

    private int totalActionsCount = 0;

    private LocalDateTime startedAt = LocalDateTime.now();

    private LocalDateTime endedAt;

    @ElementCollection
    private List<String> events = new ArrayList<>();

    /** Alias pour getLawCase() — compatibilité avec les services. */
    public Case getCurrentCase() {
        return lawCase;
    }

    public void addEvent(String event) {
        if (events == null) events = new ArrayList<>();
        events.add(event);
    }

    public enum TrialStatus {
        IN_PROGRESS,
        GUILTY,
        NOT_GUILTY
    }

    public enum TrialPhase {
        OPENING_STATEMENTS,
        PROSECUTION_CASE,
        DEFENSE_CASE,
        CROSS_EXAMINATION,
        CLOSING_ARGUMENTS,
        VERDICT
    }
}
