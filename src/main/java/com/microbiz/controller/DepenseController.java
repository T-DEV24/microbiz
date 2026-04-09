package com.microbiz.controller;

import com.microbiz.model.Depense;
import com.microbiz.security.TenantContext;
import com.microbiz.service.DepenseService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/depenses")
public class DepenseController {

    @Autowired private DepenseService depenseService;
    @Autowired private CurrencyRateService currencyRateService;

    @GetMapping
    public String liste(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            Model model,
            RedirectAttributes ra) {

        List<Depense> depenses;
        Double totalFiltre = null;

        if (debut != null && fin != null) {
            if (fin.isBefore(debut)) {
                ra.addFlashAttribute("erreur", "La date de fin doit être postérieure ou égale à la date de début.");
                return "redirect:/depenses";
            }
            depenses    = depenseService.getDepensesParPeriode(debut, fin);
            totalFiltre = depenseService.getTotalParPeriode(debut, fin);
            model.addAttribute("debut", debut);
            model.addAttribute("fin",   fin);
            model.addAttribute("filtreActif", true);
        } else {
            depenses = depenseService.findAll();
            model.addAttribute("filtreActif", false);
        }

        model.addAttribute("depenses",      depenses);
        model.addAttribute("totalDepenses", depenseService.getTotalDepenses());
        model.addAttribute("depensesMois",  depenseService.getDepensesDuMois());
        model.addAttribute("parCategorie",  depenseService.getDepensesParCategorie());
        model.addAttribute("totalFiltre",   totalFiltre);
        model.addAttribute("devises", currencyRateService.getSupportedCurrencies());
        model.addAttribute("devisePrincipale", currencyRateService.getBaseCurrency());
        model.addAttribute("ratesToBase", currencyRateService.getRatesToBase());
        return "depenses";
    }

    @PostMapping("/sauvegarder")
    public String sauvegarder(@ModelAttribute Depense depense, RedirectAttributes ra) {
        if (depense.getMontant() == null || depense.getMontant() <= 0) {
            ra.addFlashAttribute("erreur", "Le montant doit être positif.");
            return "redirect:/depenses";
        }
        String deviseNormalisee = currencyRateService.normalizeCurrency(depense.getDevise());
        depense.setMontant(currencyRateService.fromBase(
                currencyRateService.toBase(depense.getMontant(), deviseNormalisee),
                deviseNormalisee
        ));
        depense.setDevise(deviseNormalisee);
        depense.setTenantKey(TenantContext.getTenant());
        depenseService.save(depense);
        ra.addFlashAttribute("succes",
                "Dépense de "
                        + String.format("%,.0f", depense.getMontant()).replace(',', ' ')
                        + " " + depense.getDevise() + " enregistrée !");
        return "redirect:/depenses";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        depenseService.deleteById(id);
        ra.addFlashAttribute("succes", "Dépense supprimée.");
        return "redirect:/depenses";
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportXlsx(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        List<Depense> depenses = (debut != null && fin != null)
                ? depenseService.getDepensesParPeriode(debut, fin)
                : depenseService.findAll();
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Depenses");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Description");
            header.createCell(2).setCellValue("Catégorie");
            header.createCell(3).setCellValue("Devise");
            header.createCell(4).setCellValue("Montant");
            int i = 1;
            for (Depense d : depenses) {
                Row row = sheet.createRow(i++);
                row.createCell(0).setCellValue(d.getDateDepense() != null ? d.getDateDepense().toString() : "");
                row.createCell(1).setCellValue(d.getDescription() != null ? d.getDescription() : "");
                row.createCell(2).setCellValue(d.getCategorie() != null ? d.getCategorie() : "");
                row.createCell(3).setCellValue(d.getDevise() != null ? d.getDevise() : "XAF");
                row.createCell(4).setCellValue(d.getMontant() != null ? d.getMontant() : 0);
            }
            for (int c = 0; c < 5; c++) sheet.autoSizeColumn(c);
            workbook.write(baos);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=depenses-" + LocalDate.now() + ".xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Erreur export XLSX dépenses", e);
        }
    }
}
