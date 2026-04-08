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
    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(role);
    }
}