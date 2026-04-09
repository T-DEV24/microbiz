package com.microbiz.service;

import com.microbiz.model.Facture;
import com.microbiz.model.FactureLigne;
import com.microbiz.model.EntrepriseSettings;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
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
import java.io.ByteArrayOutputStream;

@Service
@Transactional
public class FactureService {

    @Autowired private FactureRepository factureRepository;
    @Autowired private EmailNotificationService emailNotificationService;
    @Autowired private EntrepriseSettingsService entrepriseSettingsService;
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

    public Facture findById(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));
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
        Facture saved = factureRepository.save(facture);
        if (statut == Facture.StatutFacture.ENVOYEE) {
            emailNotificationService.sendFactureEnvoyee(saved);
        }
        return saved;
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

    public byte[] genererPdf(Long id) {
        Facture facture = findById(id);
        EntrepriseSettings settings = entrepriseSettingsService.getSettings();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            com.lowagie.text.Document doc = new com.lowagie.text.Document(PageSize.A4, 36, 36, 42, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            if (settings.getLogo() != null && settings.getLogo().length > 0) {
                Image logo = Image.getInstance(settings.getLogo());
                logo.scaleToFit(100, 50);
                doc.add(logo);
            }
            doc.add(new Paragraph(settings.getNomEntreprise(), new Font(Font.HELVETICA, 14, Font.BOLD)));
            doc.add(new Paragraph("Adresse: " + (settings.getAdresse() != null ? settings.getAdresse() : "—")));
            doc.add(new Paragraph("SIRET: " + (settings.getSiret() != null ? settings.getSiret() : "—")
                    + " | RCCM: " + (settings.getRccm() != null ? settings.getRccm() : "—")));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Document " + facture.getNumero() + " - " + facture.getType()));
            doc.add(new Paragraph("Client: " + facture.getClientNom()));
            doc.add(new Paragraph("Date émission: " + facture.getDateEmission()));
            doc.add(new Paragraph("Échéance: " + (facture.getDateEcheance() != null ? facture.getDateEcheance() : "—")));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("Description");
            table.addCell("Qté");
            table.addCell("PU");
            table.addCell("Total");
            for (FactureLigne l : facture.getLignes()) {
                table.addCell(l.getDescription());
                table.addCell(String.valueOf(l.getQuantite()));
                table.addCell(String.valueOf(l.getPrixUnitaire()));
                table.addCell(String.format("%,.0f", l.getTotalLigne()).replace(',', ' '));
            }
            doc.add(table);
            doc.add(new Paragraph("Montant TTC: " + String.format("%,.0f F", facture.getMontantTtc()).replace(',', ' ')));
            if (settings.getMentionsLegales() != null && !settings.getMentionsLegales().isBlank()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Mentions légales: " + settings.getMentionsLegales()));
            }
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF facture", e);
        }
    }
}
