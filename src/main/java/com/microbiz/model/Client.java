package com.microbiz.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
@Entity
@Table(name = "client")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false)
    private String nom;
    private String telephone;
    @Email(message = "Email invalide")
    private String email;
    @Column(name = "date_inscription")
    private LocalDate dateInscription;
    // Remplit automatiquement la date a la creation
    @PrePersist
    public void prePersist() {
        if (dateInscription == null)
            dateInscription = LocalDate.now();
    }
}