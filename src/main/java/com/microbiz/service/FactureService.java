package com.microbiz.service;

import com.microbiz.model.Facture;
import com.microbiz.repository.FactureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class FactureService {

    @Autowired private FactureRepository factureRepository;

    public List<Facture> findAll() {
        return factureRepository.findAll();
    }

    public Facture create(Facture facture) {
        if (facture.getNumero() == null || facture.getNumero().isBlank()) {
            facture.setNumero(nextNumero(facture.getType() != null ? facture.getType() : Facture.TypeDocument.FACTURE));
        }
        if (facture.getDateEmission() == null) {
            facture.setDateEmission(LocalDate.now());
        }
        if (facture.getStatut() == null) {
            facture.setStatut(Facture.StatutFacture.BROUILLON);
        }
        return factureRepository.save(facture);
    }

    public Facture updateStatut(Long id, Facture.StatutFacture statut) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));
        facture.setStatut(statut);
        return factureRepository.save(facture);
    }

    private String nextNumero(Facture.TypeDocument type) {
        String prefix = switch (type) {
            case DEVIS -> "DEV";
            case AVOIR -> "AV";
            default -> "FAC";
        };

        long next = factureRepository.findTopByOrderByIdDesc()
                .map(f -> f.getId() + 1)
                .orElse(1L);

        String numero;
        do {
            numero = prefix + "-" + LocalDate.now().getYear() + "-" + String.format("%05d", next++);
        } while (factureRepository.existsByNumero(numero));

        return numero;
    }
}
