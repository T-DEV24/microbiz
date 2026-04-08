package com.microbiz.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "facture")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facture {

    public enum TypeDocument {
        DEVIS,
        FACTURE,
        AVOIR
    }

    public enum StatutFacture {
        BROUILLON,
        ENVOYEE,
        PAYEE,
        IMPAYEE,
        ANNULEE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDocument type = TypeDocument.FACTURE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutFacture statut = StatutFacture.BROUILLON;

    @NotBlank(message = "Le nom du client est obligatoire")
    @Column(nullable = false)
    private String clientNom;

    @DecimalMin(value = "0.0")
    @Column(nullable = false)
    private Double montantTtc;

    private LocalDate dateEmission;

    private LocalDate dateEcheance;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (dateEmission == null) dateEmission = LocalDate.now();
    }
}
