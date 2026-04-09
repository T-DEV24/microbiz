package com.microbiz.controller;

import com.microbiz.service.CurrencyRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/devises")
public class CurrencyController {

    @Autowired private CurrencyRateService currencyRateService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("baseCurrency", currencyRateService.getBaseCurrency());
        model.addAttribute("rates", currencyRateService.getRatesToBase());
        return "devises";
    }

    @PostMapping("/rate")
    public String updateRate(@RequestParam String code,
                             @RequestParam Double rate,
                             RedirectAttributes ra) {
        currencyRateService.updateRate(code, rate != null ? rate : 0.0);
        ra.addFlashAttribute("succes", "Taux mis à jour pour " + code.toUpperCase());
        return "redirect:/devises";
    }
}
