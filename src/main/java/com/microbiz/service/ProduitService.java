package com.microbiz.service;
import com.microbiz.model.Produit;
import com.microbiz.security.TenantContext;
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
    @org.springframework.beans.factory.annotation.Value("${microbiz.stock-alert.threshold:10}")
    private int stockAlertThreshold;
    @Autowired
    private ProduitRepository produitRepository;
    public List<Produit> findAll()    {
        return produitRepository.findByDeletedAtIsNullAndTenantKeyOrderByNomAsc(TenantContext.getTenant());
    }
    public Page<Produit> findAll(Pageable pageable) { return produitRepository.findByDeletedAtIsNullAndTenantKey(TenantContext.getTenant(), pageable); }
    public Page<Produit> findTrash(Pageable pageable) { return produitRepository.findByDeletedAtIsNotNullAndTenantKey(TenantContext.getTenant(), pageable); }
    public Page<Produit> rechercherActifs(String q, Pageable pageable) {
        String tenant = TenantContext.getTenant();
        if (q == null || q.isBlank()) return produitRepository.findByDeletedAtIsNullAndTenantKey(tenant, pageable);
        return produitRepository.findByDeletedAtIsNullAndNomContainingIgnoreCaseAndTenantKey(q.trim(), tenant, pageable);
    }
    public Optional<Produit> findById(Long id) { return produitRepository.findByIdAndTenantKey(id, TenantContext.getTenant()); }
    public Produit save(Produit p)   {
        if (p.getTenantKey() == null || p.getTenantKey().isBlank()) {
            p.setTenantKey(TenantContext.getTenant());
        }
        return produitRepository.save(p);
    }
    public void deleteById(Long id)   {
        Produit p = produitRepository.findByIdAndTenantKey(id, TenantContext.getTenant())
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        p.setDeletedAt(LocalDateTime.now());
        produitRepository.save(p);
    }
    public void restoreById(Long id) {
        Produit p = produitRepository.findByIdAndTenantKey(id, TenantContext.getTenant())
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        p.setDeletedAt(null);
        produitRepository.save(p);
    }
    public long countAll()             { return produitRepository.countByTenantKeyAndDeletedAtIsNull(TenantContext.getTenant()); }
    public List<Produit> findByCategorie(String cat) {
        return produitRepository.findByCategorieAndTenantKey(cat, TenantContext.getTenant());
    }
    public List<Produit> rechercher(String nom) {
        return produitRepository.findByNomContainingIgnoreCaseAndTenantKey(nom, TenantContext.getTenant());
    }
    public List<Produit> getProduitsStockBas() {
        return produitRepository.findProduitsStockBas(TenantContext.getTenant(), stockAlertThreshold);
    }
    // Decremente le stock quand une vente est enregistree
    public void decrementerStock(Long produitId, int quantite) {
        Produit p = produitRepository.findByIdAndTenantKey(produitId, TenantContext.getTenant())
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        int nvStock = p.getStockActuel() - quantite;
        if (nvStock < 0) throw new RuntimeException("Stock insuffisant pour : " + p.getNom());
        p.setStockActuel(nvStock);
        produitRepository.save(p);
    }
}
