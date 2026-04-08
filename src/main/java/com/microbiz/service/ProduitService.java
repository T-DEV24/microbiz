package com.microbiz.service;
import com.microbiz.model.Produit;
import com.microbiz.repository.ProduitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service
@Transactional
public class ProduitService {
    @Autowired
    private ProduitRepository produitRepository;
    public List<Produit> findAll()    {
        return produitRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null)
                .toList();
    }
    public Page<Produit> findAll(Pageable pageable) { return produitRepository.findByDeletedAtIsNull(pageable); }
    public Page<Produit> findTrash(Pageable pageable) { return produitRepository.findByDeletedAtIsNotNull(pageable); }
    public Page<Produit> rechercherActifs(String q, Pageable pageable) {
        if (q == null || q.isBlank()) return produitRepository.findByDeletedAtIsNull(pageable);
        return produitRepository.findByDeletedAtIsNullAndNomContainingIgnoreCase(q.trim(), pageable);
    }
    public Optional<Produit> findById(Long id) { return produitRepository.findById(id); }
    public Produit save(Produit p)   { return produitRepository.save(p); }
    public void deleteById(Long id)   {
        Produit p = produitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        p.setDeletedAt(LocalDateTime.now());
        produitRepository.save(p);
    }
    public void restoreById(Long id) {
        Produit p = produitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        p.setDeletedAt(null);
        produitRepository.save(p);
    }
    public long countAll()             { return produitRepository.count(); }
    public List<Produit> findByCategorie(String cat) {
        return produitRepository.findByCategorie(cat);
    }
    public List<Produit> rechercher(String nom) {
        return produitRepository.findByNomContainingIgnoreCase(nom);
    }
    public List<Produit> getProduitsStockBas() {
        return produitRepository.findProduitsStockBas(10).stream()
                .filter(p -> p.getDeletedAt() == null)
                .toList();
    }
    // Decremente le stock quand une vente est enregistree
    public void decrementerStock(Long produitId, int quantite) {
        Produit p = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        int nvStock = p.getStockActuel() - quantite;
        if (nvStock < 0) throw new RuntimeException("Stock insuffisant pour : " + p.getNom());
        p.setStockActuel(nvStock);
        produitRepository.save(p);
    }
}
