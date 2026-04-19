package com.microbiz.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        PAIEMENT_PARTIEL,
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

    @DecimalMin(value = "0.0")
    @Column(nullable = false)
    private Double montantHt = 0.0;

    @DecimalMin(value = "0.0")
    @Column(nullable = false)
    private Double montantTva = 0.0;

    @DecimalMin(value = "0.0")
    @Column(nullable = false)
    private Double remisePourcent = 0.0;

    @Column(length = 8, nullable = false)
    private String devise = "XAF";
    @Column(name = "tenant_key", nullable = false)
    private String tenantKey = "default";

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FactureLigne> lignes = new ArrayList<>();

    private LocalDate dateEmission;

    private LocalDate dateEcheance;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (dateEmission == null) dateEmission = LocalDate.now();
    }

    public void addLigne(FactureLigne ligne) {
        if (ligne == null) return;
        lignes.add(ligne);
        ligne.setFacture(this);
    }
}
