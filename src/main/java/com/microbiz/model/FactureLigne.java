package com.microbiz.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "facture_ligne")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactureLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Facture facture;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @NotBlank
    @Column(nullable = false)
    private String description;

    @Min(1)
    @Column(nullable = false)
    private Integer quantite;

    @DecimalMin(value = "0.0")
    @Column(nullable = false)
    private Double prixUnitaire;

    @DecimalMin(value = "0.0")
    @Column(nullable = false)
    @Builder.Default
    private Double remise = 0.0;

    public double getTotalLigne() {
        double q = quantite != null ? quantite : 0;
        double pu = prixUnitaire != null ? prixUnitaire : 0;
        double brut = q * pu;
        double remisePct = remise != null ? remise : 0;
        return brut - (brut * (remisePct / 100.0));
    }
}
