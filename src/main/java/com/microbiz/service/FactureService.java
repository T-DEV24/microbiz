package com.microbiz.service;

import com.microbiz.model.Facture;
import com.microbiz.model.FactureLigne;
import com.microbiz.repository.FactureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FactureService {

    @Autowired private FactureRepository factureRepository;
    private static final Map<Facture.StatutFacture, EnumSet<Facture.StatutFacture>> TRANSITIONS_AUTORISEES =
            new EnumMap<>(Facture.StatutFacture.class);

    static {
        TRANSITIONS_AUTORISEES.put(Facture.StatutFacture.BROUILLON,
                EnumSet.of(Facture.StatutFacture.ENVOYEE, Facture.StatutFacture.ANNULEE));
        TRANSITIONS_AUTORISEES.put(Facture.StatutFacture.ENVOYEE,
                EnumSet.of(Facture.StatutFacture.PAYEE, Facture.StatutFacture.IMPAYEE, Facture.StatutFacture.ANNULEE));
        TRANSITIONS_AUTORISEES.put(Facture.StatutFacture.IMPAYEE,
                EnumSet.of(Facture.StatutFacture.PAYEE, Facture.StatutFacture.ANNULEE));
        TRANSITIONS_AUTORISEES.put(Facture.StatutFacture.PAYEE, EnumSet.noneOf(Facture.StatutFacture.class));
        TRANSITIONS_AUTORISEES.put(Facture.StatutFacture.ANNULEE, EnumSet.noneOf(Facture.StatutFacture.class));
    }

    public List<Facture> findAll() {
        return factureRepository.findAll();
    }

    public Page<Facture> search(String q,
                                Facture.StatutFacture statut,
                                LocalDate debut,
                                LocalDate fin,
                                Pageable pageable) {
        return factureRepository.search(q, statut, debut, fin, pageable);
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
        recalculerMontant(facture);
        return factureRepository.save(facture);
    }

    public Facture updateStatut(Long id, Facture.StatutFacture statut) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));
        validerTransitionStatut(facture.getStatut(), statut);
        facture.setStatut(statut);
        return factureRepository.save(facture);
    }

    private void validerTransitionStatut(Facture.StatutFacture actuel, Facture.StatutFacture cible) {
        if (cible == null) {
            throw new RuntimeException("Le statut cible est obligatoire.");
        }
        if (actuel == cible) {
            return;
        }
        EnumSet<Facture.StatutFacture> transitions = TRANSITIONS_AUTORISEES.getOrDefault(actuel,
                EnumSet.noneOf(Facture.StatutFacture.class));
        if (!transitions.contains(cible)) {
            throw new RuntimeException("Transition invalide : " + actuel + " -> " + cible + ".");
        }
    }

    private void recalculerMontant(Facture facture) {
        if (facture.getLignes() == null || facture.getLignes().isEmpty()) {
            if (facture.getMontantTtc() == null || facture.getMontantTtc() < 0) {
                throw new RuntimeException("Le montant TTC doit être positif.");
            }
            return;
        }
        double total = 0.0;
        for (FactureLigne ligne : facture.getLignes()) {
            if (ligne == null) continue;
            ligne.setFacture(facture);
            total += ligne.getTotalLigne();
        }
        facture.setMontantTtc(Math.max(total, 0.0));
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
