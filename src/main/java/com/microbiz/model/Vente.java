package com.microbiz.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
@Entity
@Table(name = "vente")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Relation : plusieurs ventes pour un meme produit
    @ManyToOne
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;
    // Relation : plusieurs ventes pour un meme client (nullable = vente anonyme)
    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;
    @NotNull  @Min(1)
    private Integer quantite;
    @NotNull  @DecimalMin("0.0")
    @Column(name = "prix_unitaire")
    private Double prixUnitaire;
    @Column(name = "date_vente")
    private LocalDate dateVente;
    // Remplit automatiquement la date a la creation
    @PrePersist
    public void prePersist() {
        if (dateVente == null)
            dateVente = LocalDate.now();
    }
    // Methode metier : montant total = quantite x prix
    public Double getMontantTotal() {
        if (quantite == null || prixUnitaire == null) return 0.0;
        return quantite * prixUnitaire;
    }
}
