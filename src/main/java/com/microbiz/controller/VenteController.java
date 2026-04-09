package com.microbiz.controller;

import com.microbiz.model.*;
import com.microbiz.service.*;
import com.microbiz.security.TenantContext;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/ventes")
public class VenteController {

    @Autowired private VenteService   venteService;
    @Autowired private ProduitService produitService;
    @Autowired private ClientService  clientService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private CurrencyRateService currencyRateService;

    @GetMapping
    public String liste(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateVente") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            Model model,
            RedirectAttributes ra) {

        List<String> allowedSorts = List.of("dateVente", "prixUnitaire", "quantite", "id");
        String sortField = allowedSorts.contains(sort) ? sort : "dateVente";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(5, size), 100), Sort.by(direction, sortField));

        Page<Vente> ventesPage;
        Double caFiltre = null;
        boolean filtreActif;

        if (debut != null && fin != null) {
            if (fin.isBefore(debut)) {
                ra.addFlashAttribute("erreur", "La date de fin doit être postérieure ou égale à la date de début.");
                return "redirect:/ventes";
            }
            ventesPage = venteService.getVentesFiltrees(debut, fin, q, pageable);
            model.addAttribute("debut", debut);
            model.addAttribute("fin",   fin);
            filtreActif = true;
        } else {
            ventesPage = venteService.getVentesFiltrees(null, null, q, pageable);
            filtreActif = false;
        }

        String recherche = q == null ? "" : q.trim();
        if (!recherche.isEmpty()) {
            filtreActif = true;
        }

        double caVisible = ventesPage.getContent().stream()
                .mapToDouble(Vente::getMontantTotal)
                .sum();
        if (filtreActif) {
            caFiltre = caVisible;
        }

        model.addAttribute("ventes",   ventesPage.getContent());
        model.addAttribute("ventesPage", ventesPage);
        model.addAttribute("produits", produitService.findAll());
        model.addAttribute("clients",  clientService.findAll());
        model.addAttribute("caJour",   venteService.getCADuJour());
        model.addAttribute("nbVentes", venteService.getNbTransactionsDuJour());
        model.addAttribute("caFiltre", caFiltre);
        model.addAttribute("filtreActif", filtreActif);
        model.addAttribute("q", recherche);
        model.addAttribute("sort", sortField);
        model.addAttribute("dir", direction.name().toLowerCase());
        model.addAttribute("size", pageable.getPageSize());
        model.addAttribute("devises", currencyRateService.getSupportedCurrencies());
        model.addAttribute("devisePrincipale", currencyRateService.getBaseCurrency());
        model.addAttribute("ratesToBase", currencyRateService.getRatesToBase());
        return "ventes";
    }

    @PostMapping("/enregistrer")
    public String enregistrer(
            @RequestParam Long    produitId,
            @RequestParam(required = false) Long clientId,
            @RequestParam Integer quantite,
            @RequestParam Double  prixUnitaire,
            @RequestParam(defaultValue = "XAF") String devise,
            RedirectAttributes ra) {

        try {
            Produit produit = produitService.findById(produitId)
                    .orElseThrow(() -> new RuntimeException("Produit introuvable."));

            if (quantite <= 0)
                throw new RuntimeException("La quantité doit être au moins 1.");
            if (prixUnitaire <= 0)
                throw new RuntimeException("Le prix unitaire est invalide.");
            int stockActuel = produit.getStockActuel() == null ? 0 : produit.getStockActuel();
            if (stockActuel < quantite)
                throw new RuntimeException("Stock insuffisant — " + stockActuel + " unité(s) disponible(s).");

            Vente vente = new Vente();
            vente.setProduit(produit);
            vente.setQuantite(quantite);
            String deviseNormalizee = currencyRateService.normalizeCurrency(devise);
            double prixBase = currencyRateService.toBase(prixUnitaire, deviseNormalizee);
            double prixUnitaireNormalise = currencyRateService.fromBase(prixBase, deviseNormalizee);

            vente.setPrixUnitaire(prixUnitaireNormalise);
            vente.setDevise(deviseNormalizee);
            vente.setTenantKey(TenantContext.getTenant());
            if (clientId != null)
                vente.setClient(clientService.findById(clientId).orElse(null));

            Vente saved = venteService.enregistrerVente(vente);
            auditLogService.log("CREATE", "VENTE", saved.getId(), "Enregistrement vente");

            int stockRestant = stockActuel - quantite;
            double total = quantite * prixUnitaireNormalise;
            ra.addFlashAttribute("succes",
                    "Vente enregistrée — " + quantite + " × « " + produit.getNom()
                            + " » = " + String.format("%,.2f", total).replace(',', ' ')
                            + " " + vente.getDevise() + ". Stock restant : " + stockRestant + " unité(s).");

        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/ventes";
    }

    // AMÉLIORATION 2 : suppression restaure le stock + message explicite
    @PostMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            venteService.supprimerVente(id);
            auditLogService.log("DELETE", "VENTE", id, "Annulation vente + restauration stock");
            ra.addFlashAttribute("succes",
                    "Vente annulée — le stock du produit a été automatiquement restauré.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/ventes";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "dateVente") String sort,
                                            @RequestParam(defaultValue = "desc") String dir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = resolveSortField(sort);
        Page<Vente> ventes = venteService.getVentesFiltrees(debut, fin, q, PageRequest.of(0, 1000, Sort.by(direction, sortField)));

        StringBuilder csv = new StringBuilder("id,date,produit,client,quantite,prix_unitaire,devise,montant\n");
        for (Vente v : ventes.getContent()) {
            csv.append(v.getId()).append(",")
                    .append(v.getDateVente()).append(",")
                    .append(escape(v.getProduit() != null ? v.getProduit().getNom() : "")).append(",")
                    .append(escape(v.getClient() != null ? v.getClient().getNom() : "Anonyme")).append(",")
                    .append(v.getQuantite()).append(",")
                    .append(v.getPrixUnitaire()).append(",")
                    .append(v.getDevise() != null ? v.getDevise() : "XAF").append(",")
                    .append(v.getMontantTotal())
                    .append("\n");
        }
        auditLogService.log("EXPORT_CSV", "VENTE", null, "Export ventes filtrées");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ventes-" + LocalDate.now() + ".csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/export.pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "dateVente") String sort,
                                            @RequestParam(defaultValue = "desc") String dir) {
        try {
            Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortField = resolveSortField(sort);
            Page<Vente> ventes = venteService.getVentesFiltrees(debut, fin, q, PageRequest.of(0, 500, Sort.by(direction, sortField)));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 42, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Color primary = new Color(37, 99, 235);
            Color muted = new Color(107, 114, 128);
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, primary);
            Font infoFont = new Font(Font.HELVETICA, 10, Font.NORMAL, muted);

            Paragraph title = new Paragraph("MicroBiz Pro — Export Ventes", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4f);
            doc.add(title);

            String filtreDate = (debut != null && fin != null) ? " • Période : " + debut + " au " + fin : "";
            Paragraph info = new Paragraph("Généré le " + LocalDate.now() + " • " + ventes.getTotalElements() + " vente(s)" + filtreDate, infoFont);
            info.setAlignment(Element.ALIGN_CENTER);
            info.setSpacingAfter(16f);
            doc.add(info);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 2.1f, 2f, 0.7f, 0.8f, 1f});
            table.addCell("Date");
            table.addCell("Produit");
            table.addCell("Client");
            table.addCell("Qté");
            table.addCell("Devise");
            table.addCell("Montant");
            styleHeaderRow(table, primary);

            boolean odd = false;
            for (Vente v : ventes.getContent()) {
                addDataCell(table, v.getDateVente() != null ? v.getDateVente().toString() : "", odd);
                addDataCell(table, v.getProduit() != null ? v.getProduit().getNom() : "", odd);
                addDataCell(table, v.getClient() != null ? v.getClient().getNom() : "Anonyme", odd);
                addDataCell(table, String.valueOf(v.getQuantite()), odd);
                addDataCell(table, v.getDevise() != null ? v.getDevise() : "XAF", odd);
                addDataCell(table, String.format(Locale.FRANCE, "%,.2f", v.getMontantTotal()).replace(',', ' '), odd);
                odd = !odd;
            }
            doc.add(table);
            doc.close();

            auditLogService.log("EXPORT_PDF", "VENTE", null, "Export PDF ventes filtrées");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ventes-" + LocalDate.now() + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Erreur export PDF ventes", e);
        }
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportXlsx(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "dateVente") String sort,
                                             @RequestParam(defaultValue = "desc") String dir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = resolveSortField(sort);
        Page<Vente> ventes = venteService.getVentesFiltrees(debut, fin, q, PageRequest.of(0, 2000, Sort.by(direction, sortField)));
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Ventes");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Produit");
            header.createCell(2).setCellValue("Client");
            header.createCell(3).setCellValue("Quantité");
            header.createCell(4).setCellValue("Devise");
            header.createCell(5).setCellValue("Montant");

            int i = 1;
            for (Vente v : ventes.getContent()) {
                Row row = sheet.createRow(i++);
                row.createCell(0).setCellValue(v.getDateVente() != null ? v.getDateVente().toString() : "");
                row.createCell(1).setCellValue(v.getProduit() != null ? v.getProduit().getNom() : "");
                row.createCell(2).setCellValue(v.getClient() != null ? v.getClient().getNom() : "Anonyme");
                row.createCell(3).setCellValue(v.getQuantite() != null ? v.getQuantite() : 0);
                row.createCell(4).setCellValue(v.getDevise() != null ? v.getDevise() : "XAF");
                row.createCell(5).setCellValue(v.getMontantTotal());
            }
            for (int c = 0; c < 6; c++) sheet.autoSizeColumn(c);
            workbook.write(baos);
            auditLogService.log("EXPORT_XLSX", "VENTE", null, "Export ventes filtrées");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ventes-" + LocalDate.now() + ".xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Erreur export XLSX ventes", e);
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String resolveSortField(String sort) {
        List<String> allowedSorts = List.of("dateVente", "prixUnitaire", "quantite", "id");
        return allowedSorts.contains(sort) ? sort : "dateVente";
    }

    private void styleHeaderRow(PdfPTable table, Color bgColor) {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        for (int i = 0; i < table.getNumberOfColumns(); i++) {
            PdfPCell old = table.getRow(0).getCells()[i];
            PdfPCell header = new PdfPCell(new Phrase(old.getPhrase().getContent(), headerFont));
            header.setBackgroundColor(bgColor);
            header.setBorder(Rectangle.NO_BORDER);
            header.setPadding(8f);
            table.getRow(0).getCells()[i] = header;
        }
    }

    private void addDataCell(PdfPTable table, String value, boolean oddRow) {
        Font rowFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(31, 41, 55));
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", rowFont));
        cell.setPadding(7f);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(229, 231, 235));
        if (oddRow) {
            cell.setBackgroundColor(new Color(249, 250, 251));
        }
        table.addCell(cell);
    }
}
