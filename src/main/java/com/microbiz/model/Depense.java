package com.microbiz.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
@Entity
@Table(name = "depense")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Depense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "La description est obligatoire")
    private String description;
    @NotBlank(message = "La categorie est obligatoire")
    private String categorie;
    // Valeurs : Matieres premieres / Loyer / Transport / Salaires / Divers
    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.0")
    private Double montant;
    @Column(name = "date_depense")
    private LocalDate dateDepense;
    // Remplit automatiquement la date a la creation
    @PrePersist
    public void prePersist() {
        if (dateDepense == null)
            dateDepense = LocalDate.now();
    }
}