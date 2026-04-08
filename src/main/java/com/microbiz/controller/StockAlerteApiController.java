package com.microbiz.controller;

import com.microbiz.model.Produit;
import com.microbiz.service.StockAlerteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock-alertes")
public class StockAlerteApiController {

    @Autowired private StockAlerteService stockAlerteService;

    @GetMapping
    public List<Produit> getAlertes() {
        return stockAlerteService.getProduitsEnAlerte();
    }
}
