package com.microbiz.repository;
import com.microbiz.model.Produit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    // Spring genere le SQL automatiquement depuis le nom de methode
    List<Produit> findByCategorieAndTenantKey(String categorie, String tenantKey);
    List<Produit> findByNomContainingIgnoreCaseAndTenantKey(String nom, String tenantKey);
    Page<Produit> findByDeletedAtIsNullAndTenantKey(String tenantKey, Pageable pageable);
    Page<Produit> findByDeletedAtIsNotNullAndTenantKey(String tenantKey, Pageable pageable);
    Page<Produit> findByDeletedAtIsNullAndNomContainingIgnoreCaseAndTenantKey(String nom, String tenantKey, Pageable pageable);
    Optional<Produit> findByIdAndTenantKey(Long id, String tenantKey);
    // Requete JPQL custom : produits avec stock bas
    @Query("SELECT p FROM Produit p WHERE p.tenantKey = :tenantKey AND p.stockActuel <= :seuil AND p.deletedAt IS NULL ORDER BY p.stockActuel ASC")
    List<Produit> findProduitsStockBas(String tenantKey, int seuil);
    List<Produit> findByDeletedAtIsNullAndTenantKeyOrderByNomAsc(String tenantKey);
    long countByStockActuelGreaterThanAndTenantKey(int stock, String tenantKey);
    long countByTenantKeyAndDeletedAtIsNull(String tenantKey);
}
