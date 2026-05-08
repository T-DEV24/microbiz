package com.microbiz.controller;

import com.microbiz.service.CurrencyRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/devises")
public class CurrencyController {

    @Autowired private CurrencyRateService currencyRateService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("baseCurrency", currencyRateService.getBaseCurrency());
        model.addAttribute("rates", currencyRateService.getRatesToBase());
        model.addAttribute("lastRefreshAt", currencyRateService.getLastRefreshAt());
        return "devises";
    }

    @GetMapping("/rates")
    @ResponseBody
    public Map<String, Object> rates() {
        return Map.of(
                "baseCurrency", currencyRateService.getBaseCurrency(),
                "rates", currencyRateService.getRatesToBase(),
                "lastRefreshAt", currencyRateService.getLastRefreshAt() != null ? currencyRateService.getLastRefreshAt().toString() : ""
        );
    }

    @PostMapping("/refresh")
    public String refresh(RedirectAttributes ra) {
        currencyRateService.refreshRatesNow();
        ra.addFlashAttribute("succes", "Taux actualisés automatiquement depuis la source configurée.");
        return "redirect:/devises";
    }
}
