package com.microbiz.repository;
import com.microbiz.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    // Spring genere le SQL automatiquement depuis le nom de methode
    List<Produit> findByCategorie(String categorie);
    List<Produit> findByNomContainingIgnoreCase(String nom);
    // Requete JPQL custom : produits avec stock bas
    @Query("SELECT p FROM Produit p WHERE p.stockActuel <= :seuil ORDER BY p.stockActuel ASC")
    List<Produit> findProduitsStockBas(int seuil);
    long countByStockActuelGreaterThan(int stock);
}