package com.microbiz.controller;

import com.microbiz.model.Utilisateur;
import com.microbiz.repository.UtilisateurRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String index(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String role,
                        Model model) {
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll();
        if (q != null && !q.isBlank()) {
            String query = q.trim().toLowerCase();
            utilisateurs = utilisateurs.stream()
                    .filter(u -> (u.getNom() != null && u.getNom().toLowerCase().contains(query))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(query)))
                    .toList();
        }
        if (role != null && !role.isBlank()) {
            utilisateurs = utilisateurs.stream()
                    .filter(u -> role.equals(u.getRole()))
                    .toList();
        }
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
        if (utilisateurRepository.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("erreur", "Un utilisateur avec cet email existe déjà.");
            return "redirect:/utilisateurs";
        }
        Utilisateur u = Utilisateur.builder()
                .nom(nom.trim())
                .email(email.trim().toLowerCase())
                .motDePasse(passwordEncoder.encode(motDePasse))
                .role(resolveRole(role))
                .build();
        utilisateurRepository.save(u);
        ra.addFlashAttribute("succes", "Utilisateur ajouté avec succès.");
        return "redirect:/utilisateurs";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) String role) {
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll();
        if (q != null && !q.isBlank()) {
            String query = q.trim().toLowerCase();
            utilisateurs = utilisateurs.stream()
                    .filter(u -> (u.getNom() != null && u.getNom().toLowerCase().contains(query))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(query)))
                    .toList();
        }
        if (role != null && !role.isBlank()) {
            utilisateurs = utilisateurs.stream()
                    .filter(u -> role.equals(u.getRole()))
                    .toList();
        }

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

    private String resolveRole(String role) {
        if ("ROLE_ADMIN".equals(role)) return "ROLE_ADMIN";
        if ("ROLE_COMMERCIAL".equals(role)) return "ROLE_COMMERCIAL";
        return "ROLE_USER";
    }
}
