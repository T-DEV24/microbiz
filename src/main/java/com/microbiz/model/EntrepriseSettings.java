package com.microbiz.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "entreprise_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrepriseSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomEntreprise;

    private String siret;
    private String rccm;
    private String adresse;
    private String mentionsLegales;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] logo;

    private String logoContentType;
}
