package model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Classe abstraite représentant toute personne impliquée dans une affaire.
 * Witness, Suspect et Lawyer héritent de cette classe.
 */
@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
public abstract class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;     // Nom complet de la personne
    private int age;
    private String gender;   // "M", "F", etc.
    private String description; // Description physique ou background narratif
}
