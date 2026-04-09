package com.microbiz.controller;

import com.microbiz.model.EntrepriseSettings;
import com.microbiz.service.EntrepriseSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/entreprise")
public class EntrepriseSettingsController {

    @Autowired private EntrepriseSettingsService settingsService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("settings", settingsService.getSettings());
        return "entreprise-settings";
    }

    @PostMapping
    public String save(@ModelAttribute("settings") EntrepriseSettings settings,
                       @RequestParam(name = "logoFile", required = false) MultipartFile logoFile,
                       RedirectAttributes ra) {
        settingsService.save(settings, logoFile);
        ra.addFlashAttribute("succes", "Informations entreprise mises à jour.");
        return "redirect:/entreprise";
    }

    @GetMapping("/logo")
    @ResponseBody
    public ResponseEntity<byte[]> logo() {
        EntrepriseSettings settings = settingsService.getSettings();
        if (settings.getLogo() == null || settings.getLogo().length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(settings.getLogoContentType() != null ? settings.getLogoContentType() : MediaType.IMAGE_PNG_VALUE))
                .body(settings.getLogo());
    }
}
