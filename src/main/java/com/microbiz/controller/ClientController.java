package com.microbiz.controller;

import com.microbiz.model.Client;
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

@Controller
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @GetMapping
    public String liste(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "15") int size,
                        @RequestParam(defaultValue = "nom") String sort,
                        @RequestParam(defaultValue = "asc") String dir,
                        Model model) {
        String sortField = switch (sort) {
            case "email", "telephone", "dateInscription", "id" -> sort;
            default -> "nom";
        };
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(5, size), 100), Sort.by(direction, sortField));
        Page<Client> clientsPage = clientService.findAll(pageable);

        model.addAttribute("clients", clientsPage.getContent());
        model.addAttribute("clientsPage", clientsPage);
        model.addAttribute("client",  new Client());
        model.addAttribute("sort", sortField);
        model.addAttribute("dir", direction.name().toLowerCase());
        model.addAttribute("size", pageable.getPageSize());
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

    @PostMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        clientService.deleteById(id);
        redirectAttrs.addFlashAttribute("succes", "Client supprime.");
        return "redirect:/clients";
    }
}
