package com.microbiz.controller;

import com.microbiz.model.Client;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.microbiz.service.AuditLogService;
import com.microbiz.service.ClientService;
import jakarta.validation.Valid;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;
    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public String liste(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "15") int size,
                        @RequestParam(defaultValue = "nom") String sort,
                        @RequestParam(defaultValue = "asc") String dir,
                        @RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "false") boolean trash,
                        Model model) {
        String sortField = switch (sort) {
            case "email", "telephone", "dateInscription", "id" -> sort;
            default -> "nom";
        };
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(5, size), 100), Sort.by(direction, sortField));
        Page<Client> clientsPage = trash
                ? clientService.findTrash(pageable)
                : clientService.rechercherActifs(q, pageable);

        model.addAttribute("clients", clientsPage.getContent());
        model.addAttribute("clientsPage", clientsPage);
        model.addAttribute("client",  new Client());
        model.addAttribute("sort", sortField);
        model.addAttribute("dir", direction.name().toLowerCase());
        model.addAttribute("size", pageable.getPageSize());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("trash", trash);
        return "clients";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("client", new Client());
        return "client-form";
    }

    @GetMapping("/modifier/{id}")
    public String modifier(@PathVariable Long id, Model model) {
        Client client = clientService.findById(id)
                .orElseThrow(() -> new RuntimeException("Client introuvable"));
        model.addAttribute("client", client);
        return "client-form";
    }

    @PostMapping("/sauvegarder")
    public String sauvegarder(@Valid @ModelAttribute Client client,
                              BindingResult result,
                              RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) return "client-form";
        boolean isNew = client.getId() == null;
        Client saved = clientService.save(client);
        auditLogService.log(isNew ? "CREATE" : "UPDATE", "CLIENT", saved.getId(), saved.getNom());
        redirectAttrs.addFlashAttribute("succes", "Client sauvegarde !");
        return "redirect:/clients";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        clientService.deleteById(id);
        auditLogService.log("SOFT_DELETE", "CLIENT", id, "Suppression logique");
        redirectAttrs.addFlashAttribute("succes", "Client supprime.");
        return "redirect:/clients";
    }

    @PostMapping("/restaurer/{id}")
    public String restaurer(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        clientService.restoreById(id);
        auditLogService.log("RESTORE", "CLIENT", id, "Restauration depuis corbeille");
        redirectAttrs.addFlashAttribute("succes", "Client restauré.");
        return "redirect:/clients?trash=true";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) String q) {
        Page<Client> page = clientService.rechercherActifs(q, PageRequest.of(0, 500, Sort.by("nom").ascending()));
        StringBuilder csv = new StringBuilder("id,nom,telephone,email,date_inscription\n");
        for (Client c : page.getContent()) {
            csv.append(c.getId()).append(",")
                    .append(escape(c.getNom())).append(",")
                    .append(escape(c.getTelephone())).append(",")
                    .append(escape(c.getEmail())).append(",")
                    .append(c.getDateInscription() != null ? c.getDateInscription() : "")
                    .append("\n");
        }
        auditLogService.log("EXPORT_CSV", "CLIENT", null, "Export clients");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=clients-" + LocalDate.now() + ".csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/export.pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestParam(required = false) String q) {
        try {
            Page<Client> page = clientService.rechercherActifs(q, PageRequest.of(0, 500, Sort.by("nom").ascending()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 42, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            BaseColor primary = new BaseColor(37, 99, 235);
            BaseColor muted = new BaseColor(107, 114, 128);
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, primary);
            Font infoFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, muted);

            Paragraph title = new Paragraph("MicroBiz Pro — Export Clients", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4f);
            doc.add(title);

            Paragraph info = new Paragraph("Généré le " + LocalDate.now() + " • " + page.getTotalElements() + " client(s)", infoFont);
            info.setAlignment(Element.ALIGN_CENTER);
            info.setSpacingAfter(16f);
            doc.add(info);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 1.5f, 2.2f, 1.4f});
            table.addCell("Nom");
            table.addCell("Téléphone");
            table.addCell("Email");
            table.addCell("Inscription");
            styleHeaderRow(table, primary);

            boolean odd = false;
            for (Client c : page.getContent()) {
                addDataCell(table, c.getNom(), odd);
                addDataCell(table, c.getTelephone(), odd);
                addDataCell(table, c.getEmail(), odd);
                addDataCell(table, c.getDateInscription() != null ? c.getDateInscription().toString() : "", odd);
                odd = !odd;
            }
            doc.add(table);
            doc.close();
            auditLogService.log("EXPORT_PDF", "CLIENT", null, "Export clients");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=clients-" + LocalDate.now() + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Erreur export PDF clients", e);
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
