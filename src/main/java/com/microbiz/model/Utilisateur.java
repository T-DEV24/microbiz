package com.microbiz.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
@Entity
@Table(name = "utilisateur")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;
    @Email  @NotBlank
    @Column(unique = true, nullable = false)
    private String email;
    @NotBlank
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;
    // ROLE_ADMIN, ROLE_GERANT, ROLE_USER, ROLE_COMPTABLE ou ROLE_COMMERCIAL
    @NotBlank
    private String role;

    @Column(name = "tenant_key", nullable = false)
    private String tenantKey = "default";

    @PrePersist
    public void prePersist() {
        if (tenantKey == null || tenantKey.isBlank()) {
            tenantKey = "default";
        }
    }
    public boolean isAdmin() {
        return PmeRole.ADMIN.matches(role);
    }
    public boolean isGerant() {
        return PmeRole.GERANT.matches(role);
    }
    public boolean isComptable() {
        return PmeRole.COMPTABLE.matches(role);
    }
    public boolean isCommercial() {
        return PmeRole.COMMERCIAL.matches(role);
    }
    public String getRoleLabel() {
        return PmeRole.fromAuthority(role)
                .map(PmeRole::getLabel)
                .orElse(PmeRole.USER.getLabel());
    }
}
