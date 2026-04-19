package com.microbiz.service;

import com.microbiz.model.Facture;
import com.microbiz.model.FactureLigne;
import com.microbiz.model.EntrepriseSettings;
import com.microbiz.security.TenantContext;
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
import java.util.UUID;
import java.io.ByteArrayOutputStream;

@Service
@Transactional
public class FactureService {

    @Autowired private FactureRepository factureRepository;
    @Autowired private EmailNotificationService emailNotificationService;
    @Autowired private EntrepriseSettingsService entrepriseSettingsService;
    @Autowired private PaiementService paiementService;
    private static final Map<Facture.StatutFacture, EnumSet<Facture.StatutFacture>> TRANSITIONS_AUTORISEES =
            new EnumMap<>(Facture.StatutFacture.class);

    static {
        TRANSITIONS_AUTORISEES.put(Facture.StatutFacture.BROUILLON,
                EnumSet.of(Facture.StatutFacture.ENVOYEE, Facture.StatutFacture.ANNULEE));
        TRANSITIONS_AUTORISEES.put(Facture.StatutFacture.ENVOYEE,
                EnumSet.of(Facture.StatutFacture.PAIEMENT_PARTIEL, Facture.StatutFacture.PAYEE, Facture.StatutFacture.IMPAYEE, Facture.StatutFacture.ANNULEE));
        TRANSITIONS_AUTORISEES.put(Facture.StatutFacture.PAIEMENT_PARTIEL,
                EnumSet.of(Facture.StatutFacture.PAYEE, Facture.StatutFacture.IMPAYEE, Facture.StatutFacture.ANNULEE));
        TRANSITIONS_AUTORISEES.put(Facture.StatutFacture.IMPAYEE,
                EnumSet.of(Facture.StatutFacture.PAIEMENT_PARTIEL, Facture.StatutFacture.PAYEE, Facture.StatutFacture.ANNULEE));
        TRANSITIONS_AUTORISEES.put(Facture.StatutFacture.PAYEE, EnumSet.noneOf(Facture.StatutFacture.class));
        TRANSITIONS_AUTORISEES.put(Facture.StatutFacture.ANNULEE, EnumSet.noneOf(Facture.StatutFacture.class));
    }

    public List<Facture> findAll() {
        return factureRepository.findAllByTenantKeyOrderByDateEmissionDesc(TenantContext.getTenant());
    }

    public Facture findById(Long id) {
        return factureRepository.findByIdAndTenantKey(id, TenantContext.getTenant())
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));
    }

    public Page<Facture> search(String q,
                                Facture.StatutFacture statut,
                                LocalDate debut,
                                LocalDate fin,
                                Pageable pageable) {
        String tenant = TenantContext.getTenant();
        return factureRepository.search(tenant, q, statut, debut, fin, pageable);
    }

    public Facture create(Facture facture) {
        if (facture.getDateEmission() == null) {
            facture.setDateEmission(LocalDate.now());
        }
        if (facture.getStatut() == null) {
            facture.setStatut(Facture.StatutFacture.BROUILLON);
        }
        if (facture.getDevise() == null || facture.getDevise().isBlank()) {
            facture.setDevise("XAF");
        }
        facture.setTenantKey(TenantContext.getTenant());
        recalculerMontant(facture);

        if (facture.getNumero() != null && !facture.getNumero().isBlank()) {
            return factureRepository.save(facture);
        }

        facture.setNumero("TMP-" + UUID.randomUUID());
        Facture created = factureRepository.save(facture);
        created.setNumero(formatNumero(created.getType(), created.getId()));
        return factureRepository.save(created);
    }

    public Facture updateStatut(Long id, Facture.StatutFacture statut) {
        Facture facture = findById(id);
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
            facture.setMontantHt(Math.max(facture.getMontantTtc(), 0.0));
            facture.setMontantTva(0.0);
            facture.setRemisePourcent(Math.max(facture.getRemisePourcent() == null ? 0.0 : facture.getRemisePourcent(), 0.0));
            return;
        }
        double totalHt = 0.0;
        double totalTva = 0.0;
        for (FactureLigne ligne : facture.getLignes()) {
            if (ligne == null) continue;
            ligne.setFacture(facture);
            totalHt += ligne.getMontantHt();
            totalTva += ligne.getMontantTva();
        }
        double remiseFacturePct = Math.max(facture.getRemisePourcent() == null ? 0.0 : facture.getRemisePourcent(), 0.0);
        double montantRemise = totalHt * (remiseFacturePct / 100.0);
        double htApresRemise = Math.max(totalHt - montantRemise, 0.0);
        double ratio = totalHt <= 0 ? 1.0 : (htApresRemise / totalHt);
        double tvaApresRemise = Math.max(totalTva * ratio, 0.0);

        facture.setMontantHt(htApresRemise);
        facture.setMontantTva(tvaApresRemise);
        facture.setMontantTtc(Math.max(htApresRemise + tvaApresRemise, 0.0));
    }

    private String formatNumero(Facture.TypeDocument type, Long sequence) {
        String prefix = switch (type) {
            case DEVIS -> "DEV";
            case AVOIR -> "AV";
            default -> "FAC";
        };
        return prefix + "-" + LocalDate.now().getYear() + "-" + String.format("%05d", sequence);
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
            doc.add(new Paragraph("Devise: " + (facture.getDevise() != null ? facture.getDevise() : "XAF")));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.addCell("Description");
            table.addCell("Qté");
            table.addCell("PU");
            table.addCell("Remise %");
            table.addCell("TVA %");
            table.addCell("Total TTC");
            for (FactureLigne l : facture.getLignes()) {
                table.addCell(l.getDescription());
                table.addCell(String.valueOf(l.getQuantite()));
                table.addCell(String.valueOf(l.getPrixUnitaire()));
                table.addCell(String.format("%,.2f", l.getRemise() == null ? 0.0 : l.getRemise()).replace(',', ' '));
                table.addCell(String.format("%,.2f", l.getTauxTva() == null ? 0.0 : l.getTauxTva()).replace(',', ' '));
                table.addCell(String.format("%,.0f", l.getTotalLigne()).replace(',', ' '));
            }
            doc.add(table);
            doc.add(new Paragraph("Sous-total HT: " + String.format("%,.0f", facture.getMontantHt()).replace(',', ' ') + " " + facture.getDevise()));
            doc.add(new Paragraph("TVA: " + String.format("%,.0f", facture.getMontantTva()).replace(',', ' ') + " " + facture.getDevise()));
            doc.add(new Paragraph("Remise facture (%): " + String.format("%,.2f", facture.getRemisePourcent() == null ? 0.0 : facture.getRemisePourcent()).replace(',', ' ')));
            doc.add(new Paragraph("Montant TTC: " + String.format("%,.0f", facture.getMontantTtc()).replace(',', ' ') + " " + facture.getDevise()));

            List<com.microbiz.model.Paiement> paiements = paiementService.findByFacture(facture.getId());
            if (!paiements.isEmpty()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Historique des encaissements", new Font(Font.HELVETICA, 12, Font.BOLD)));
                PdfPTable paiementTable = new PdfPTable(4);
                paiementTable.setWidthPercentage(100);
                paiementTable.addCell("Date");
                paiementTable.addCell("Mode");
                paiementTable.addCell("Référence");
                paiementTable.addCell("Montant");
                for (com.microbiz.model.Paiement p : paiements) {
                    paiementTable.addCell(p.getDateEncaissement() != null ? p.getDateEncaissement().toString() : "—");
                    paiementTable.addCell(p.getModePaiement() != null ? p.getModePaiement().name() : "—");
                    paiementTable.addCell(p.getReference() != null && !p.getReference().isBlank() ? p.getReference() : "—");
                    paiementTable.addCell(String.format("%,.0f %s", p.getMontant(), p.getDevise()).replace(',', ' '));
                }
                doc.add(paiementTable);
                double totalEncaisse = paiementService.getTotalEncaisseByFacture(facture.getId());
                double reste = paiementService.getResteAPayer(facture.getId());
                doc.add(new Paragraph("Total encaissé (base): " + String.format("%,.0f", totalEncaisse).replace(',', ' ') + " " + facture.getDevise()));
                doc.add(new Paragraph("Reste à payer (base): " + String.format("%,.0f", reste).replace(',', ' ') + " " + facture.getDevise()));
            }
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
