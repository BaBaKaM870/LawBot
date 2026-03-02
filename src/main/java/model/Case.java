package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "law_case")
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private CrimeType crimeType;

    private LocalDate crimeDate;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "suspect_id")
    private Suspect suspect;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "case_id")
    private List<Witness> witnesses;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "case_id")
    private List<Evidence> evidences;

    /** Les membres du jury assignés à cette affaire. */
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "case_jury_id")
    private List<JuryMember> jury;

    public enum CrimeType {
        MURDER,
        THEFT,
        FRAUD,
        ASSAULT,
        CORRUPTION
    }
}
