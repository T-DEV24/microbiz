package com.microbiz.service;

import com.microbiz.model.*;
import com.microbiz.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class VenteService {

    @Autowired private VenteRepository   venteRepository;
    @Autowired private ProduitRepository produitRepository;

    public List<Vente> findAll()   { return venteRepository.findAllByOrderByDateVenteDesc(); }
    public long countAll()         { return venteRepository.count(); }
    public Optional<Vente> findById(Long id) { return venteRepository.findById(id); }

    public List<Vente> getVentesRecentes() {
        return venteRepository.findTop20ByOrderByDateVenteDesc();
    }

    public List<Vente> getVentesParPeriode(LocalDate debut, LocalDate fin) {
        return venteRepository.findByDateVenteBetweenOrderByDateVenteDesc(debut, fin);
    }

    public Double getCAParPeriode(LocalDate debut, LocalDate fin) {
        Double ca = venteRepository.calculerCAParPeriode(debut, fin);
        return ca != null ? ca : 0.0;
    }

    public Double getCADuJour() {
        Double ca = venteRepository.calculerCADuJour(LocalDate.now());
        return ca != null ? ca : 0.0;
    }

    public long getNbTransactionsDuJour() {
        return venteRepository.countByDateVente(LocalDate.now());
    }

    /** Enregistrer une vente ET décrémenter le stock */
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

        p.setStockActuel(stockActuel - quantite);
        produitRepository.save(p);
        return venteRepository.save(vente);
    }

    /** Supprimer une vente ET restaurer le stock — AMÉLIORATION 2 */
    public void supprimerVente(Long id) {
        Vente vente = venteRepository.findById(id)
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
        List<Object[]> rows = venteRepository.findTopProduits(PageRequest.of(0, n));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("produit",  (Produit) row[0]);
            item.put("quantite", ((Number) row[1]).longValue());
            item.put("ca",       ((Number) row[2]).doubleValue());
            result.add(item);
        }
        return result;
    }
}
