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
    // ROLE_ADMIN  ou  ROLE_USER
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
        return "ROLE_ADMIN".equals(role);
    }
    public boolean isCommercial() {
        return "ROLE_COMMERCIAL".equals(role);
    }
}
