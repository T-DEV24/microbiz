package com.microbiz.controller;

import com.microbiz.model.Utilisateur;
import com.microbiz.repository.UtilisateurRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/utilisateurs")
public class UtilisateurController {

    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("utilisateurs", utilisateurRepository.findAll());
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
                .role("ROLE_ADMIN".equals(role) ? "ROLE_ADMIN" : "ROLE_USER")
                .build();
        utilisateurRepository.save(u);
        ra.addFlashAttribute("succes", "Utilisateur ajouté avec succès.");
        return "redirect:/utilisateurs";
    }
}
