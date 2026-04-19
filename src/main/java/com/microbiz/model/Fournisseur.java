package com.microbiz.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "fournisseur")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nom;

    private String telephone;

    private String email;

    @Column(name = "tenant_key", nullable = false, length = 100)
    private String tenantKey = "default";

    @PrePersist
    public void prePersist() {
        if (tenantKey == null || tenantKey.isBlank()) {
            tenantKey = "default";
        }
    }
}
