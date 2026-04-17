package com.microbiz.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "produit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Le nom du produit est obligatoire")
    @Column(nullable = false)
    private String nom;
    private String categorie;
    @DecimalMin(value = "0.0")
    @Column(name = "prix_vente")
    private Double prixVente;
    @DecimalMin(value = "0.0")
    @Column(name = "cout_revient")
    private Double coutRevient;
    @Min(0)
    @Column(name = "stock_actuel")
    private Integer stockActuel;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "tenant_key", nullable = false)
    private String tenantKey = "default";
    // Methode metier : calcul de la marge en %
    public Double getMarge() {
        if (prixVente == null || coutRevient == null || prixVente == 0) return 0.0;
        return ((prixVente - coutRevient) / prixVente) * 100;
    }
    // Methode metier : stock bas si <= seuil
    public boolean isStockBas(int seuil) {
        return stockActuel != null && stockActuel <= seuil;
    }

    @PrePersist
    public void prePersist() {
        if (tenantKey == null || tenantKey.isBlank()) {
            tenantKey = "default";
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
