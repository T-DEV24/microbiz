package com.microbiz.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "achat_commande")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchatCommande {

    public enum StatutAchat {
        BROUILLON,
        COMMANDEE,
        RECEPTION_PARTIELLE,
        RECEPTIONNEE,
        ANNULEE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    @Min(1)
    @Column(nullable = false)
    private Integer quantite;

    @Column(nullable = false)
    private Double coutUnitaire;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantiteRecue = 0;

    private LocalDate dateCommande;

    private LocalDate dateReception;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAchat statut = StatutAchat.BROUILLON;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (dateCommande == null) dateCommande = LocalDate.now();
    }
}
