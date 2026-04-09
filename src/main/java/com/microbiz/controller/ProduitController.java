package com.microbiz.controller;

import com.microbiz.model.Produit;
import com.microbiz.model.Depense;
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
import com.microbiz.service.AuditLogService;
import com.microbiz.service.DepenseService;
import com.microbiz.service.ProduitService;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@RequestMapping("/produits")
public class ProduitController {

    @Autowired private ProduitService produitService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private DepenseService depenseService;

    @GetMapping
    public String liste(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "15") int size,
                        @RequestParam(defaultValue = "nom") String sort,
                        @RequestParam(defaultValue = "asc") String dir,
                        @RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "false") boolean trash,
                        Model model) {
        String sortField = switch (sort) {
            case "categorie", "prixVente", "stockActuel", "id" -> sort;
            default -> "nom";
        };
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(5, size), 100), Sort.by(direction, sortField));
        Page<Produit> produitsPage = trash
                ? produitService.findTrash(pageable)
                : produitService.rechercherActifs(q, pageable);

        model.addAttribute("produits",  produitsPage.getContent());
        model.addAttribute("produitsPage", produitsPage);
        model.addAttribute("stockBas",  produitService.getProduitsStockBas());
        model.addAttribute("produit",   new Produit());
        model.addAttribute("sort", sortField);
        model.addAttribute("dir", direction.name().toLowerCase());
        model.addAttribute("size", pageable.getPageSize());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("trash", trash);
        return "produits";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("produit", new Produit());
        return "produit-form";
    }

    @GetMapping("/modifier/{id}")
    public String modifier(@PathVariable Long id, Model model) {
        model.addAttribute("produit", produitService.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable")));
        return "produit-form";
    }

    @PostMapping("/sauvegarder")
    public String sauvegarder(@Valid @ModelAttribute Produit produit,
                              BindingResult result,
                              Model model,
                              RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("produits", produitService.findAll());
            model.addAttribute("stockBas", produitService.getProduitsStockBas());
            return "produits";
        }
        boolean isNew = (produit.getId() == null);
        int oldStock = 0;
        if (!isNew) {
            Produit existing = produitService.findById(produit.getId())
                    .orElseThrow(() -> new RuntimeException("Produit introuvable"));
            oldStock = existing.getStockActuel() != null ? existing.getStockActuel() : 0;
        }
        Produit saved = produitService.save(produit);
        auditLogService.log(isNew ? "CREATE" : "UPDATE", "PRODUIT", saved.getId(), saved.getNom());

        int newStock = saved.getStockActuel() != null ? saved.getStockActuel() : 0;
        int stockAjoute = isNew ? Math.max(newStock, 0) : Math.max(newStock - oldStock, 0);
        double coutRevient = saved.getCoutRevient() != null ? saved.getCoutRevient() : 0.0;
        double montantAppro = stockAjoute * coutRevient;
        boolean depenseAutoGeneree = false;
        if (stockAjoute > 0 && montantAppro > 0) {
            Depense depense = Depense.builder()
                    .description("Approvisionnement produit : " + saved.getNom() + " (" + stockAjoute + " unité(s))")
                    .categorie("Matieres premieres")
                    .montant(montantAppro)
                    .build();
            depenseService.save(depense);
            depenseAutoGeneree = true;
            auditLogService.log("AUTO_DEPENSE", "PRODUIT", saved.getId(),
                    "Dépense auto générée : " + Math.round(montantAppro) + " FCFA");
        }

        String messageSucces = isNew
                ? "Produit « " + produit.getNom() + " » ajouté !"
                : "Produit « " + produit.getNom() + " » modifié !";
        if (depenseAutoGeneree) {
            messageSucces += " Dépense d'approvisionnement enregistrée ("
                    + String.format("%,.0f", montantAppro).replace(',', ' ')
                    + " FCFA).";
        }
        ra.addFlashAttribute("succes", messageSucces);
        return "redirect:/produits";
    }

    // AMÉLIORATION 3 : réapprovisionnement inline
    @PostMapping("/reapprovisionner/{id}")
    public String reapprovisionner(@PathVariable Long id,
                                   @RequestParam int quantite,
                                   RedirectAttributes ra) {
        if (quantite <= 0) {
            ra.addFlashAttribute("erreur", "La quantité doit être positive.");
            return "redirect:/produits";
        }
        Produit p = produitService.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        p.setStockActuel(p.getStockActuel() + quantite);
        produitService.save(p);

        double coutRevient = p.getCoutRevient() != null ? p.getCoutRevient() : 0.0;
        double montantAppro = quantite * coutRevient;
        if (montantAppro > 0) {
            Depense depense = Depense.builder()
                    .description("Réapprovisionnement : " + p.getNom() + " (+" + quantite + ")")
                    .categorie("Matieres premieres")
                    .montant(montantAppro)
                    .build();
            depenseService.save(depense);
        }

        auditLogService.log("RESTOCK", "PRODUIT", p.getId(), "Quantité ajoutée: " + quantite);
        ra.addFlashAttribute("succes",
                "Stock de « " + p.getNom() + " » mis à jour : +" + quantite
                        + " unités (total : " + p.getStockActuel() + ")."
                        + (montantAppro > 0
                        ? " Dépense auto: " + String.format("%,.0f", montantAppro).replace(',', ' ') + " FCFA."
                        : ""));
        return "redirect:/produits";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            produitService.deleteById(id);
            auditLogService.log("SOFT_DELETE", "PRODUIT", id, "Suppression logique");
            ra.addFlashAttribute("succes", "Produit supprimé.");
        } catch (Exception e) {
            ra.addFlashAttribute("erreur",
                    "Impossible de supprimer ce produit — il est lié à des ventes existantes.");
        }
        return "redirect:/produits";
    }

    @PostMapping("/restaurer/{id}")
    public String restaurer(@PathVariable Long id, RedirectAttributes ra) {
        produitService.restoreById(id);
        auditLogService.log("RESTORE", "PRODUIT", id, "Restauration depuis corbeille");
        ra.addFlashAttribute("succes", "Produit restauré.");
        return "redirect:/produits?trash=true";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) String q) {
        Page<Produit> page = produitService.rechercherActifs(q, PageRequest.of(0, 500, Sort.by("nom").ascending()));
        StringBuilder csv = new StringBuilder("id,nom,categorie,prix_vente,cout_revient,stock\n");
        for (Produit p : page.getContent()) {
            csv.append(p.getId()).append(",")
                    .append(escape(p.getNom())).append(",")
                    .append(escape(p.getCategorie())).append(",")
                    .append(p.getPrixVente() != null ? p.getPrixVente() : "").append(",")
                    .append(p.getCoutRevient() != null ? p.getCoutRevient() : "").append(",")
                    .append(p.getStockActuel() != null ? p.getStockActuel() : "")
                    .append("\n");
        }
        auditLogService.log("EXPORT_CSV", "PRODUIT", null, "Export produits");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=produits-" + LocalDate.now() + ".csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/export.pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestParam(required = false) String q) {
        try {
            Page<Produit> page = produitService.rechercherActifs(q, PageRequest.of(0, 500, Sort.by("nom").ascending()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 42, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            BaseColor primary = new BaseColor(37, 99, 235);
            BaseColor muted = new BaseColor(107, 114, 128);
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, primary);
            Font infoFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, muted);

            Paragraph title = new Paragraph("MicroBiz Pro — Export Produits", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4f);
            doc.add(title);

            Paragraph info = new Paragraph("Généré le " + LocalDate.now() + " • " + page.getTotalElements() + " produit(s)", infoFont);
            info.setAlignment(Element.ALIGN_CENTER);
            info.setSpacingAfter(16f);
            doc.add(info);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 1.5f, 1.2f, 1.2f, 0.9f});
            table.addCell("Nom");
            table.addCell("Catégorie");
            table.addCell("Prix");
            table.addCell("Coût");
            table.addCell("Stock");
            styleHeaderRow(table, primary);

            boolean odd = false;
            for (Produit p : page.getContent()) {
                addDataCell(table, p.getNom(), odd);
                addDataCell(table, p.getCategorie(), odd);
                addDataCell(table, p.getPrixVente() != null ? String.valueOf(p.getPrixVente()) : "", odd);
                addDataCell(table, p.getCoutRevient() != null ? String.valueOf(p.getCoutRevient()) : "", odd);
                addDataCell(table, p.getStockActuel() != null ? String.valueOf(p.getStockActuel()) : "", odd);
                odd = !odd;
            }
            doc.add(table);
            doc.close();
            auditLogService.log("EXPORT_PDF", "PRODUIT", null, "Export produits");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=produits-" + LocalDate.now() + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Erreur export PDF produits", e);
        }
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportXlsx(@RequestParam(required = false) String q) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Produits");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nom");
            header.createCell(1).setCellValue("Catégorie");
            header.createCell(2).setCellValue("Prix");
            header.createCell(3).setCellValue("Coût");
            header.createCell(4).setCellValue("Stock");

            Page<Produit> page = produitService.rechercherActifs(q, PageRequest.of(0, 2000, Sort.by("nom").ascending()));
            int rowNum = 1;
            for (Produit p : page.getContent()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getNom() != null ? p.getNom() : "");
                row.createCell(1).setCellValue(p.getCategorie() != null ? p.getCategorie() : "");
                row.createCell(2).setCellValue(p.getPrixVente() != null ? p.getPrixVente() : 0);
                row.createCell(3).setCellValue(p.getCoutRevient() != null ? p.getCoutRevient() : 0);
                row.createCell(4).setCellValue(p.getStockActuel() != null ? p.getStockActuel() : 0);
            }
            for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);
            workbook.write(baos);
            auditLogService.log("EXPORT_XLSX", "PRODUIT", null, "Export produits XLSX");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=produits-" + LocalDate.now() + ".xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Erreur export XLSX produits", e);
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private void styleHeaderRow(PdfPTable table, BaseColor bgColor) {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
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
        Font rowFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(31, 41, 55));
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", rowFont));
        cell.setPadding(7f);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new BaseColor(229, 231, 235));
        if (oddRow) {
            cell.setBackgroundColor(new BaseColor(249, 250, 251));
        }
        table.addCell(cell);
    }
}
