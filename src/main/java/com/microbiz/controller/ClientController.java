package com.microbiz.controller;

import com.microbiz.model.Client;
import com.microbiz.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("client",  new Client());
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
        clientService.save(client);
        redirectAttrs.addFlashAttribute("succes", "Client sauvegarde !");
        return "redirect:/clients";
    }

    @GetMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        clientService.deleteById(id);
        redirectAttrs.addFlashAttribute("succes", "Client supprime.");
        return "redirect:/clients";
    }
}