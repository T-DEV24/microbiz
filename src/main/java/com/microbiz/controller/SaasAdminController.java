package com.microbiz.controller;

import com.microbiz.service.CurrencyRateService;
import com.microbiz.model.SaasSubscription;
import com.microbiz.service.SaasSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/saas/admin")
public class SaasAdminController {

    @Autowired private CurrencyRateService currencyRateService;
    @Autowired private SaasSubscriptionService saasSubscriptionService;

    @GetMapping
    public String index(Model model) {
        String baseCurrency = currencyRateService.getBaseCurrency();
        model.addAttribute("baseCurrency", baseCurrency);
        model.addAttribute("plans", SaasSubscription.PlanCode.values());
        model.addAttribute("statuses", SaasSubscription.SubscriptionStatus.values());
        model.addAttribute("planPrices", saasSubscriptionService.getPlanPrices(baseCurrency));
        model.addAttribute("subscriptions", saasSubscriptionService.findAll());
        return "saas-admin";
    }

    @PostMapping("/subscriptions")
    public String upsertSubscription(@RequestParam String tenantKey,
                                     @RequestParam SaasSubscription.PlanCode planCode,
                                     @RequestParam(defaultValue = "ACTIVE") SaasSubscription.SubscriptionStatus status,
                                     @RequestParam(required = false) LocalDate startsAt,
                                     @RequestParam(required = false) LocalDate trialEndsAt,
                                     @RequestParam(defaultValue = "true") Boolean autoRenew,
                                     RedirectAttributes ra) {
        try {
            saasSubscriptionService.upsert(
                    tenantKey,
                    planCode,
                    status,
                    startsAt,
                    trialEndsAt,
                    autoRenew,
                    currencyRateService.getBaseCurrency()
            );
            ra.addFlashAttribute("succes", "Abonnement SaaS enregistré pour le tenant " + tenantKey + ".");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/saas/admin";
    }

    @PostMapping("/subscriptions/{tenantKey}/cancel")
    public String cancelSubscription(@PathVariable String tenantKey, RedirectAttributes ra) {
        try {
            saasSubscriptionService.cancel(tenantKey);
            ra.addFlashAttribute("succes", "Abonnement annulé pour le tenant " + tenantKey + ".");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/saas/admin";
    }
}
