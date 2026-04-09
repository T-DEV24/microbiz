package com.microbiz.controller;

import com.microbiz.service.CurrencyRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/saas/admin")
public class SaasAdminController {

    @Autowired private CurrencyRateService currencyRateService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("plans", java.util.List.of("FREE", "PRO", "BUSINESS"));
        model.addAttribute("baseCurrency", currencyRateService.getBaseCurrency());
        return "saas-admin";
    }
}
