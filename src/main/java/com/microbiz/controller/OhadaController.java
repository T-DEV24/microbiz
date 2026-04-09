package com.microbiz.controller;

import com.microbiz.service.OhadaAccountingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/comptabilite/ohada")
public class OhadaController {

    @Autowired private OhadaAccountingService ohadaAccountingService;

    @GetMapping
    public String journal(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                          Model model) {
        model.addAttribute("entries", ohadaAccountingService.genererJournal(debut, fin));
        model.addAttribute("debut", debut);
        model.addAttribute("fin", fin);
        return "compta-ohada";
    }
}
