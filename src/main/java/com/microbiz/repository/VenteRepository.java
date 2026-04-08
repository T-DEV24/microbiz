package com.microbiz.repository;

import com.microbiz.model.Vente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VenteRepository extends JpaRepository<Vente, Long> {

    List<Vente> findAllByOrderByDateVenteDesc();
    List<Vente> findTop20ByOrderByDateVenteDesc();

    List<Vente> findByDateVenteBetweenOrderByDateVenteDesc(LocalDate debut, LocalDate fin);

    @Query(
            value = "SELECT v FROM Vente v " +
                    "LEFT JOIN v.produit p " +
                    "LEFT JOIN v.client c " +
                    "WHERE (:debut IS NULL OR v.dateVente >= :debut) " +
                    "AND (:fin IS NULL OR v.dateVente <= :fin) " +
                    "AND (:q IS NULL OR :q = '' OR " +
                    "LOWER(p.nom) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
                    "LOWER(c.nom) LIKE LOWER(CONCAT('%', :q, '%')))",
            countQuery = "SELECT COUNT(v) FROM Vente v " +
                    "LEFT JOIN v.produit p " +
                    "LEFT JOIN v.client c " +
                    "WHERE (:debut IS NULL OR v.dateVente >= :debut) " +
                    "AND (:fin IS NULL OR v.dateVente <= :fin) " +
                    "AND (:q IS NULL OR :q = '' OR " +
                    "LOWER(p.nom) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
                    "LOWER(c.nom) LIKE LOWER(CONCAT('%', :q, '%')))"
    )
    Page<Vente> findByFiltres(@Param("debut") LocalDate debut,
                              @Param("fin") LocalDate fin,
                              @Param("q") String q,
                              Pageable pageable);

    @Query("SELECT COALESCE(SUM(v.quantite * v.prixUnitaire), 0) FROM Vente v WHERE v.dateVente = :date")
    Double calculerCADuJour(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(v.quantite * v.prixUnitaire), 0) FROM Vente v")
    Double calculerCATotal();

    @Query("SELECT COALESCE(SUM(v.quantite * v.prixUnitaire), 0) FROM Vente v " +
            "WHERE MONTH(v.dateVente) = :mois AND YEAR(v.dateVente) = :annee")
    Double calculerCADuMois(@Param("mois") int mois, @Param("annee") int annee);

    @Query("SELECT COALESCE(SUM(v.quantite * v.prixUnitaire), 0) FROM Vente v " +
            "WHERE v.dateVente BETWEEN :debut AND :fin")
    Double calculerCAParPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    long countByDateVente(LocalDate date);

    long countByDateVenteBetween(LocalDate debut, LocalDate fin);

    // AMÉLIORATION 1 : evolution mensuelle
    @Query("SELECT MONTH(v.dateVente), YEAR(v.dateVente), SUM(v.quantite * v.prixUnitaire) " +
            "FROM Vente v GROUP BY YEAR(v.dateVente), MONTH(v.dateVente) " +
            "ORDER BY YEAR(v.dateVente), MONTH(v.dateVente)")
    List<Object[]> getEvolutionMensuelle();

    // AMÉLIORATION 1 : evolution hebdomadaire
    @Query("SELECT WEEK(v.dateVente), YEAR(v.dateVente), SUM(v.quantite * v.prixUnitaire) " +
            "FROM Vente v WHERE v.dateVente >= :depuis " +
            "GROUP BY YEAR(v.dateVente), WEEK(v.dateVente) " +
            "ORDER BY YEAR(v.dateVente), WEEK(v.dateVente)")
    List<Object[]> getEvolutionHebdomadaire(@Param("depuis") LocalDate depuis);

    // Top produits
    @Query("SELECT v.produit, SUM(v.quantite), SUM(v.quantite * v.prixUnitaire) " +
            "FROM Vente v GROUP BY v.produit ORDER BY SUM(v.quantite) DESC")
    List<Object[]> findTopProduits(Pageable pageable);

    @Query("SELECT v.produit, SUM(v.quantite), SUM(v.quantite * v.prixUnitaire) " +
            "FROM Vente v WHERE v.dateVente BETWEEN :debut AND :fin " +
            "GROUP BY v.produit ORDER BY SUM(v.quantite) DESC")
    List<Object[]> findTopProduitsParPeriode(@Param("debut") LocalDate debut,
                                             @Param("fin") LocalDate fin,
                                             Pageable pageable);
}
