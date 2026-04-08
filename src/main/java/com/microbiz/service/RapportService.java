package com.microbiz.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class RapportService {

    @Autowired private StatistiqueService statistiqueService;
    @Autowired private VenteService venteService;
    @Autowired private DepenseService depenseService;

    public byte[] genererRapportPDF() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Couleurs
            BaseColor bleu = new BaseColor(37, 99, 235);
            BaseColor gris = new BaseColor(107, 114, 128);
            BaseColor rouge = new BaseColor(220, 38, 38);
            BaseColor vert = new BaseColor(5, 150, 105);

            // Polices
            Font fTitre = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, bleu);
            Font fSousTi = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
            Font fNormal = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
            Font fMuted = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, gris);

            // En-tête
            Paragraph titre = new Paragraph("MicroBiz Pro — Rapport Financier", fTitre);
            titre.setAlignment(Element.ALIGN_CENTER);
            titre.setSpacingAfter(4);
            doc.add(titre);

            Paragraph date = new Paragraph(
                    "Généré le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    fMuted);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            doc.add(date);

            doc.add(new LineSeparator(0.5f, 100, gris, Element.ALIGN_CENTER, -2));

            // Section KPIs
            doc.add(new Paragraph("\nIndicateurs financiers\n", fSousTi));

            double ca = statistiqueService.getChiffreAffairesTotal();
            double dep = depenseService.getTotalDepenses();
            double ben = statistiqueService.getBeneficeNet();
            double marge = statistiqueService.getMargeBeneficiaire();

            PdfPTable tableKpi = new PdfPTable(2);
            tableKpi.setWidthPercentage(100);
            tableKpi.setSpacingBefore(8);
            tableKpi.setSpacingAfter(16);

            addKpiRow(tableKpi, "Chiffre d'affaires total",
                    String.format("%,.0f FCFA", ca).replace(',', ' '), bleu, fNormal);

            addKpiRow(tableKpi, "Total des dépenses",
                    String.format("%,.0f FCFA", dep).replace(',', ' '), rouge, fNormal);

            addKpiRow(tableKpi, "Bénéfice net",
                    String.format("%,.0f FCFA", ben).replace(',', ' '), vert, fNormal);

            addKpiRow(tableKpi, "Marge bénéficiaire",
                    String.format("%.1f %%", marge),
                    marge >= 40 ? vert : (marge >= 20 ? new BaseColor(217, 119, 6) : rouge),
                    fNormal);

            doc.add(tableKpi);

            // Top produits
            doc.add(new Paragraph("Top 5 produits vendus\n", fSousTi));

            List<Map<String, Object>> top = venteService.getTopProduits(5);
            PdfPTable tableTop = new PdfPTable(4);
            tableTop.setWidthPercentage(100);
            tableTop.setSpacingBefore(8);
            tableTop.setWidths(new float[]{0.5f, 3f, 1.5f, 1.5f});

            addTableHeader(tableTop,
                    new String[]{"#", "Produit", "Unités vendues", "CA"},
                    bleu);

            int rang = 1;
            for (Map<String, Object> item : top) {

                com.microbiz.model.Produit p =
                        (com.microbiz.model.Produit) item.get("produit");

                Number qte = (Number) item.get("quantite");
                Number revenu = (Number) item.get("ca");

                addTableRow(tableTop, new String[]{
                        String.valueOf(rang++),
                        p.getNom(),
                        String.valueOf(qte.longValue()),
                        String.format("%,.0f F", revenu.doubleValue()).replace(',', ' ')
                });
            }

            doc.add(tableTop);

            doc.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF : " + e.getMessage(), e);
        }
    }

    private void addKpiRow(PdfPTable table, String label, String value,
                           BaseColor valueColor, Font fNormal) {

        PdfPCell c1 = new PdfPCell(new Phrase(label, fNormal));
        c1.setBorder(Rectangle.BOTTOM);
        c1.setPadding(8);

        Font fVal = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, valueColor);
        PdfPCell c2 = new PdfPCell(new Phrase(value, fVal));
        c2.setBorder(Rectangle.BOTTOM);
        c2.setPadding(8);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(c1);
        table.addCell(c2);
    }

    private void addTableHeader(PdfPTable table, String[] headers, BaseColor bgColor) {
        Font fh = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fh));
            cell.setBackgroundColor(bgColor);
            cell.setPadding(7);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String[] values) {
        Font fRow = new Font(Font.FontFamily.HELVETICA, 10);

        for (String v : values) {
            PdfPCell cell = new PdfPCell(new Phrase(v, fRow));
            cell.setPadding(7);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColor(new BaseColor(243, 244, 246));
            table.addCell(cell);
        }
    }
}
