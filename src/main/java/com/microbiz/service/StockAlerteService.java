package com.microbiz.service;

import com.microbiz.model.Produit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockAlerteService {

    @Autowired private ProduitService produitService;
    @Autowired private WebhookService webhookService;

    public List<Produit> getProduitsEnAlerte() {
        return produitService.getProduitsStockBas();
    }

    @Scheduled(cron = "${microbiz.stock-alert.cron:0 0 8 * * *}")
    public void notifierStockBas() {
        List<Produit> alertes = getProduitsEnAlerte();
        if (alertes.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("count", alertes.size());
        payload.put("produits", alertes.stream().map(p -> Map.of(
                "id", p.getId(),
                "nom", p.getNom(),
                "stockActuel", p.getStockActuel()
        )).toList());

        webhookService.publish("stock.low", payload);
    }
}
