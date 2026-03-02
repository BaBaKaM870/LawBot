package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "witness_id")
    private Witness witness;

    /** Nom du témoin (dénormalisé pour accès rapide). */
    private String witnessName;

    /** La question posée par le joueur. */
    private String questionAsked;

    /** La réponse du témoin. */
    @Column(length = 1000)
    private String content;

    private boolean lie;

    private int turn;
}
