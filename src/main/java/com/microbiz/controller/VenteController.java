package com.microbiz.controller;

import com.microbiz.model.*;
import com.microbiz.service.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/ventes")
public class VenteController {

    @Autowired private VenteService   venteService;
    @Autowired private ProduitService produitService;
    @Autowired private ClientService  clientService;
    @Autowired private AuditLogService auditLogService;

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
        return "ventes";
    }

    @PostMapping("/enregistrer")
    public String enregistrer(
            @RequestParam Long    produitId,
            @RequestParam(required = false) Long clientId,
            @RequestParam Integer quantite,
            @RequestParam Double  prixUnitaire,
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
            vente.setPrixUnitaire(prixUnitaire);
            if (clientId != null)
                vente.setClient(clientService.findById(clientId).orElse(null));

            venteService.enregistrerVente(vente);
            auditLogService.log("CREATE", "VENTE", vente.getId(), "Enregistrement vente");

            int stockRestant = stockActuel - quantite;
            long total = (long)(quantite * prixUnitaire);
            ra.addFlashAttribute("succes",
                    "Vente enregistrée — " + quantite + " × « " + produit.getNom()
                            + " » = " + String.format("%,d", total).replace(',', ' ')
                            + " FCFA. Stock restant : " + stockRestant + " unité(s).");

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
        Page<Vente> ventes = venteService.getVentesFiltrees(debut, fin, q, PageRequest.of(0, 1000, Sort.by(direction, sort)));

        StringBuilder csv = new StringBuilder("id,date,produit,client,quantite,prix_unitaire,montant\n");
        for (Vente v : ventes.getContent()) {
            csv.append(v.getId()).append(",")
                    .append(v.getDateVente()).append(",")
                    .append(escape(v.getProduit() != null ? v.getProduit().getNom() : "")).append(",")
                    .append(escape(v.getClient() != null ? v.getClient().getNom() : "Anonyme")).append(",")
                    .append(v.getQuantite()).append(",")
                    .append(v.getPrixUnitaire()).append(",")
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
            Page<Vente> ventes = venteService.getVentesFiltrees(debut, fin, q, PageRequest.of(0, 500, Sort.by(direction, sort)));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document();
            PdfWriter.getInstance(doc, baos);
            doc.open();
            doc.add(new Paragraph("Export ventes - " + LocalDate.now()));
            PdfPTable table = new PdfPTable(5);
            table.addCell("Date");
            table.addCell("Produit");
            table.addCell("Client");
            table.addCell("Qté");
            table.addCell("Montant");
            for (Vente v : ventes.getContent()) {
                table.addCell(v.getDateVente() != null ? v.getDateVente().toString() : "");
                table.addCell(v.getProduit() != null ? v.getProduit().getNom() : "");
                table.addCell(v.getClient() != null ? v.getClient().getNom() : "Anonyme");
                table.addCell(String.valueOf(v.getQuantite()));
                table.addCell(String.format("%.2f", v.getMontantTotal()));
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

    private String escape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
