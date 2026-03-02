package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Contradiction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "statement1_id")
    private Statement statement1;

    @ManyToOne
    @JoinColumn(name = "statement2_id")
    private Statement statement2;

    @Column(length = 1000)
    private String description;

    /** Sujet de la contradiction (ex: "alibi", "heure du crime"). */
    private String topic;

    /** Gravité de la contradiction (0.0 à 1.0). */
    private double severity = 0.5;

    private boolean detected = false;
}
