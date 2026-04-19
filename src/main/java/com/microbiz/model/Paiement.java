package com.microbiz.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "paiement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paiement {

    public enum ModePaiement {
        ESPECES,
        VIREMENT,
        MOBILE_MONEY,
        CHEQUE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Facture facture;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false)
    private Double montant;

    @Column(length = 8, nullable = false)
    private String devise = "XAF";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModePaiement modePaiement;

    @Column(name = "date_encaissement", nullable = false)
    private LocalDate dateEncaissement;

    private String reference;

    @Column(name = "tenant_key", nullable = false)
    private String tenantKey = "default";

    @PrePersist
    public void prePersist() {
        if (dateEncaissement == null) {
            dateEncaissement = LocalDate.now();
        }
        if (devise == null || devise.isBlank()) {
            devise = "XAF";
        }
        if (tenantKey == null || tenantKey.isBlank()) {
            tenantKey = "default";
        }
    }
}
