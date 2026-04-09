package com.microbiz.service;

import com.lowagie.text.BaseColor;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
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

    public byte[] genererRapportPDF(String periode, int top, LocalDate debut, LocalDate fin) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            BaseColor bleu = new BaseColor(37, 99, 235);
            BaseColor gris = new BaseColor(107, 114, 128);
            BaseColor rouge = new BaseColor(220, 38, 38);
            BaseColor vert = new BaseColor(5, 150, 105);

            Font fTitre = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, bleu);
            Font fSousTi = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
            Font fNormal = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
            Font fMuted = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, gris);

            Paragraph titre = new Paragraph("MicroBiz Pro — Rapport Financier", fTitre);
            titre.setAlignment(Element.ALIGN_CENTER);
            titre.setSpacingAfter(4);
            doc.add(titre);

            Paragraph date = new Paragraph(
                    "Généré le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    fMuted);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(4);
            doc.add(date);

            String filtre = (debut != null && fin != null)
                    ? "Période filtrée : " + debut + " au " + fin
                    : "Période : globale";
            Paragraph periodeP = new Paragraph(filtre, fMuted);
            periodeP.setAlignment(Element.ALIGN_CENTER);
            periodeP.setSpacingAfter(16);
            doc.add(periodeP);

            doc.add(new LineSeparator(0.5f, 100, gris, Element.ALIGN_CENTER, -2));

            doc.add(new Paragraph("\nIndicateurs financiers\n", fSousTi));

            double ca = (debut != null && fin != null)
                    ? statistiqueService.getChiffreAffairesParPeriode(debut, fin)
                    : statistiqueService.getChiffreAffairesTotal();
            double dep = (debut != null && fin != null)
                    ? statistiqueService.getTotalDepensesParPeriode(debut, fin)
                    : depenseService.getTotalDepenses();
            double ben = ca - dep;
            double marge = ca > 0 ? (ben / ca) * 100 : 0.0;

            PdfPTable tableKpi = new PdfPTable(2);
            tableKpi.setWidthPercentage(100);
            tableKpi.setSpacingBefore(8);
            tableKpi.setSpacingAfter(16);

            addKpiRow(tableKpi, "Chiffre d'affaires",
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

            doc.add(new Paragraph("Top " + top + " produits vendus\n", fSousTi));

            List<Map<String, Object>> topProduits = venteService.getTopProduits(top, debut, fin);
            PdfPTable tableTop = new PdfPTable(4);
            tableTop.setWidthPercentage(100);
            tableTop.setSpacingBefore(8);
            tableTop.setWidths(new float[]{0.5f, 3f, 1.5f, 1.5f});

            addTableHeader(tableTop,
                    new String[]{"#", "Produit", "Unités vendues", "CA"},
                    bleu);

            int rang = 1;
            for (Map<String, Object> item : topProduits) {
                com.microbiz.model.Produit p = (com.microbiz.model.Produit) item.get("produit");
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

    public String genererRapportCsv(String periode, int top, LocalDate debut, LocalDate fin) {
        double ca = (debut != null && fin != null)
                ? statistiqueService.getChiffreAffairesParPeriode(debut, fin)
                : statistiqueService.getChiffreAffairesTotal();
        double dep = (debut != null && fin != null)
                ? statistiqueService.getTotalDepensesParPeriode(debut, fin)
                : depenseService.getTotalDepenses();
        double ben = ca - dep;
        double marge = ca > 0 ? (ben / ca) * 100 : 0.0;

        StringBuilder csv = new StringBuilder();
        csv.append("type,indicateur,valeur\n");
        csv.append("kpi,periode,").append(debut != null && fin != null ? debut + " au " + fin : "globale").append("\n");
        csv.append("kpi,chiffre_affaires,").append(String.format("%.2f", ca)).append("\n");
        csv.append("kpi,depenses,").append(String.format("%.2f", dep)).append("\n");
        csv.append("kpi,benefice,").append(String.format("%.2f", ben)).append("\n");
        csv.append("kpi,marge_pourcent,").append(String.format("%.2f", marge)).append("\n");

        int rank = 1;
        for (Map<String, Object> item : venteService.getTopProduits(top, debut, fin)) {
            com.microbiz.model.Produit p = (com.microbiz.model.Produit) item.get("produit");
            Number quantite = (Number) item.get("quantite");
            Number revenu = (Number) item.get("ca");
            csv.append("top_produit,")
                    .append(rank++)
                    .append(" - ")
                    .append(p.getNom().replace(',', ' '))
                    .append(",")
                    .append(quantite.longValue())
                    .append(" unités / ")
                    .append(String.format("%.2f", revenu.doubleValue()))
                    .append("\n");
        }

        for (Map.Entry<String, Double> entry : statistiqueService.getEvolutionParFiltre(periode,
                debut != null ? debut : LocalDate.now().minusMonths(11).withDayOfMonth(1),
                fin != null ? fin : LocalDate.now()).entrySet()) {
            csv.append("evolution_ca,")
                    .append(entry.getKey())
                    .append(",")
                    .append(String.format("%.2f", entry.getValue()))
                    .append("\n");
        }

        return csv.toString();
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
