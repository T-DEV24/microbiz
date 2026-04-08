package com.microbiz.service;
import com.microbiz.model.Produit;
import com.microbiz.repository.ProduitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Service
@Transactional
public class ProduitService {
    @Autowired
    private ProduitRepository produitRepository;
    public List<Produit> findAll()    { return produitRepository.findAll(); }
    public Page<Produit> findAll(Pageable pageable) { return produitRepository.findAll(pageable); }
    public Optional<Produit> findById(Long id) { return produitRepository.findById(id); }
    public Produit save(Produit p)   { return produitRepository.save(p); }
    public void deleteById(Long id)   { produitRepository.deleteById(id); }
    public long countAll()             { return produitRepository.count(); }
    public List<Produit> findByCategorie(String cat) {
        return produitRepository.findByCategorie(cat);
    }
    public List<Produit> rechercher(String nom) {
        return produitRepository.findByNomContainingIgnoreCase(nom);
    }
    public List<Produit> getProduitsStockBas() {
        return produitRepository.findProduitsStockBas(10);
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
