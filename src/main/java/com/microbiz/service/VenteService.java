package com.microbiz.service;

import com.microbiz.model.*;
import com.microbiz.security.TenantContext;
import com.microbiz.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class VenteService {

    @Autowired private VenteRepository   venteRepository;
    @Autowired private ProduitRepository produitRepository;
    @Autowired private CurrencyRateService currencyRateService;
    @Autowired private EmailNotificationService emailNotificationService;
    @org.springframework.beans.factory.annotation.Value("${microbiz.stock-alert.threshold:10}")
    private int stockAlertThreshold;

    public List<Vente> findAll()   {
        String tenant = TenantContext.getTenant();
        return venteRepository.findAllByTenantKeyOrderByDateVenteDesc(tenant);
    }
    public long countAll()         { return venteRepository.countByTenantKey(TenantContext.getTenant()); }

    public long countAll(LocalDate debut, LocalDate fin) {
        String tenant = TenantContext.getTenant();
        if (debut != null && fin != null) {
            return venteRepository.countByTenantKeyAndDateVenteBetween(tenant, debut, fin);
        }
        return venteRepository.countByTenantKey(tenant);
    }
    public Optional<Vente> findById(Long id) { return venteRepository.findByIdAndTenantKey(id, TenantContext.getTenant()); }

    public List<Vente> getVentesRecentes() {
        return venteRepository.findTop20ByTenantKeyOrderByDateVenteDesc(TenantContext.getTenant());
    }

    public Page<Vente> getVentesFiltrees(LocalDate debut, LocalDate fin, String q, Pageable pageable) {
        String tenant = TenantContext.getTenant();
        return venteRepository.findByFiltres(tenant, debut, fin, q, pageable);
    }

    public List<Vente> getVentesParPeriode(LocalDate debut, LocalDate fin) {
        String tenant = TenantContext.getTenant();
        return venteRepository.findByTenantKeyAndDateVenteBetweenOrderByDateVenteDesc(tenant, debut, fin);
    }

    public Double getCAParPeriode(LocalDate debut, LocalDate fin) {
        return getVentesParPeriode(debut, fin).stream()
                .mapToDouble(v -> currencyRateService.toBase(v.getMontantTotal(), v.getDevise()))
                .sum();
    }

    public Double getCADuJour() {
        LocalDate now = LocalDate.now();
        return getVentesParPeriode(now, now).stream()
                .mapToDouble(v -> currencyRateService.toBase(v.getMontantTotal(), v.getDevise()))
                .sum();
    }

    public long getNbTransactionsDuJour() {
        LocalDate now = LocalDate.now();
        return venteRepository.countByTenantKeyAndDateVente(TenantContext.getTenant(), now);
    }

    /** Enregistrer une vente ET décrémenter le stock */
    @CacheEvict(cacheNames = {"stats_ca_total", "stats_evolution_mensuelle"}, allEntries = true)
    public Vente enregistrerVente(Vente vente) {
        Produit p = vente.getProduit();
        int stockActuel = p.getStockActuel() == null ? 0 : p.getStockActuel();
        int quantite = vente.getQuantite() == null ? 0 : vente.getQuantite();

        if (quantite <= 0) {
            throw new RuntimeException("La quantité doit être positive.");
        }
        if (stockActuel < quantite) {
            throw new RuntimeException("Stock insuffisant — " + stockActuel + " unité(s) disponible(s).");
        }
        int nouveauStock = stockActuel - quantite;
        p.setStockActuel(nouveauStock);
        produitRepository.save(p);
        if (stockActuel > stockAlertThreshold && nouveauStock <= stockAlertThreshold) {
            emailNotificationService.sendStockBas(List.of(p));
        }
        return venteRepository.save(vente);
    }

    /** Supprimer une vente ET restaurer le stock — AMÉLIORATION 2 */
    @CacheEvict(cacheNames = {"stats_ca_total", "stats_evolution_mensuelle"}, allEntries = true)
    public void supprimerVente(Long id) {
        Vente vente = venteRepository.findByIdAndTenantKey(id, TenantContext.getTenant())
                .orElseThrow(() -> new RuntimeException("Vente introuvable."));
        // Restaurer le stock avant suppression
        Produit p = vente.getProduit();
        int stockActuel = p.getStockActuel() == null ? 0 : p.getStockActuel();
        int quantite = vente.getQuantite() == null ? 0 : vente.getQuantite();
        p.setStockActuel(stockActuel + quantite);
        produitRepository.save(p);
        venteRepository.deleteById(id);
    }

    public List<Map<String, Object>> getTopProduits(int n) {
        return getTopProduits(n, null, null);
    }

    public List<Map<String, Object>> getTopProduits(int n, LocalDate debut, LocalDate fin) {
        List<Vente> ventes = (debut != null && fin != null)
                ? getVentesParPeriode(debut, fin)
                : findAll();

        Map<Produit, long[]> quantites = new LinkedHashMap<>();
        Map<Produit, Double> caConsolide = new LinkedHashMap<>();
        for (Vente vente : ventes) {
            if (vente.getProduit() == null) continue;
            Produit produit = vente.getProduit();
            long qte = vente.getQuantite() != null ? vente.getQuantite() : 0;
            double caBase = currencyRateService.toBase(vente.getMontantTotal(), vente.getDevise());
            quantites.computeIfAbsent(produit, p -> new long[]{0})[0] += qte;
            caConsolide.put(produit, caConsolide.getOrDefault(produit, 0.0) + caBase);
        }

        return caConsolide.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(n)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("produit", e.getKey());
                    item.put("quantite", quantites.get(e.getKey())[0]);
                    item.put("ca", e.getValue());
                    return item;
                })
                .toList();
    }
}
