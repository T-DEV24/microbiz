package com.microbiz.controller;

import com.microbiz.model.Utilisateur;
import com.microbiz.service.UtilisateurService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/utilisateurs")
public class UtilisateurController {

    @Autowired
    private UtilisateurService utilisateurService;

    @GetMapping
    public String index(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String role,
                        Model model) {
        List<Utilisateur> utilisateurs = utilisateurService.rechercher(q, role);
        model.addAttribute("utilisateurs", utilisateurs);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("role", role == null ? "" : role);
        return "utilisateurs";
    }

    @PostMapping("/sauvegarder")
    public String sauvegarder(@RequestParam @NotBlank String nom,
                              @RequestParam @NotBlank String email,
                              @RequestParam @NotBlank String motDePasse,
                              @RequestParam(defaultValue = "ROLE_USER") String role,
                              RedirectAttributes ra) {
        if (utilisateurService.emailExiste(email)) {
            ra.addFlashAttribute("erreur", "Un utilisateur avec cet email existe déjà.");
            return "redirect:/utilisateurs";
        }
        utilisateurService.creer(nom, email, motDePasse, role);
        ra.addFlashAttribute("succes", "Utilisateur ajouté avec succès.");
        return "redirect:/utilisateurs";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) String role) {
        List<Utilisateur> utilisateurs = utilisateurService.rechercher(q, role);

        StringBuilder csv = new StringBuilder("id,nom,email,role\n");
        for (Utilisateur u : utilisateurs) {
            csv.append(u.getId()).append(",")
                    .append(escape(u.getNom())).append(",")
                    .append(escape(u.getEmail())).append(",")
                    .append(u.getRole())
                    .append("\n");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=utilisateurs-" + LocalDate.now() + ".csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String escape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

}
