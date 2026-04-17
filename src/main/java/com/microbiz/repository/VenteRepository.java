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
    java.util.Optional<Vente> findByIdAndTenantKey(Long id, String tenantKey);

    List<Vente> findAllByTenantKeyOrderByDateVenteDesc(String tenantKey);
    List<Vente> findTop20ByTenantKeyOrderByDateVenteDesc(String tenantKey);

    List<Vente> findByTenantKeyAndDateVenteBetweenOrderByDateVenteDesc(String tenantKey, LocalDate debut, LocalDate fin);

    @Query(
            value = "SELECT v FROM Vente v " +
                    "LEFT JOIN v.produit p " +
                    "LEFT JOIN v.client c " +
                    "WHERE v.tenantKey = :tenantKey " +
                    "AND (:debut IS NULL OR v.dateVente >= :debut) " +
                    "AND (:fin IS NULL OR v.dateVente <= :fin) " +
                    "AND (:q IS NULL OR :q = '' OR " +
                    "LOWER(p.nom) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
                    "LOWER(c.nom) LIKE LOWER(CONCAT('%', :q, '%')))",
            countQuery = "SELECT COUNT(v) FROM Vente v " +
                    "LEFT JOIN v.produit p " +
                    "LEFT JOIN v.client c " +
                    "WHERE v.tenantKey = :tenantKey " +
                    "AND (:debut IS NULL OR v.dateVente >= :debut) " +
                    "AND (:fin IS NULL OR v.dateVente <= :fin) " +
                    "AND (:q IS NULL OR :q = '' OR " +
                    "LOWER(p.nom) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
                    "LOWER(c.nom) LIKE LOWER(CONCAT('%', :q, '%')))"
    )
    Page<Vente> findByFiltres(@Param("tenantKey") String tenantKey,
                              @Param("debut") LocalDate debut,
                              @Param("fin") LocalDate fin,
                              @Param("q") String q,
                              Pageable pageable);

    @Query("SELECT COALESCE(SUM(v.quantite * v.prixUnitaire), 0) FROM Vente v WHERE v.tenantKey = :tenantKey AND v.dateVente = :date")
    Double calculerCADuJour(@Param("tenantKey") String tenantKey, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(v.quantite * v.prixUnitaire), 0) FROM Vente v WHERE v.tenantKey = :tenantKey")
    Double calculerCATotal(@Param("tenantKey") String tenantKey);

    @Query("SELECT COALESCE(SUM(v.quantite * v.prixUnitaire), 0) FROM Vente v " +
            "WHERE v.tenantKey = :tenantKey AND MONTH(v.dateVente) = :mois AND YEAR(v.dateVente) = :annee")
    Double calculerCADuMois(@Param("tenantKey") String tenantKey, @Param("mois") int mois, @Param("annee") int annee);

    @Query("SELECT COALESCE(SUM(v.quantite * v.prixUnitaire), 0) FROM Vente v " +
            "WHERE v.tenantKey = :tenantKey AND v.dateVente BETWEEN :debut AND :fin")
    Double calculerCAParPeriode(@Param("tenantKey") String tenantKey, @Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    long countByTenantKeyAndDateVente(String tenantKey, LocalDate date);

    long countByTenantKeyAndDateVenteBetween(String tenantKey, LocalDate debut, LocalDate fin);

    long countByTenantKey(String tenantKey);

    // AMÉLIORATION 1 : evolution mensuelle
    @Query("SELECT MONTH(v.dateVente), YEAR(v.dateVente), SUM(v.quantite * v.prixUnitaire) " +
            "FROM Vente v WHERE v.tenantKey = :tenantKey GROUP BY YEAR(v.dateVente), MONTH(v.dateVente), v.devise " +
            "ORDER BY YEAR(v.dateVente), MONTH(v.dateVente)")
    List<Object[]> getEvolutionMensuelle(@Param("tenantKey") String tenantKey);

    // AMÉLIORATION 1 : evolution hebdomadaire
    @Query("SELECT WEEK(v.dateVente), YEAR(v.dateVente), SUM(v.quantite * v.prixUnitaire) " +
            "FROM Vente v WHERE v.tenantKey = :tenantKey AND v.dateVente >= :depuis " +
            "GROUP BY YEAR(v.dateVente), WEEK(v.dateVente), v.devise " +
            "ORDER BY YEAR(v.dateVente), WEEK(v.dateVente)")
    List<Object[]> getEvolutionHebdomadaire(@Param("tenantKey") String tenantKey, @Param("depuis") LocalDate depuis);

    // Top produits
    @Query("SELECT v.produit, SUM(v.quantite), SUM(v.quantite * v.prixUnitaire) " +
            "FROM Vente v WHERE v.tenantKey = :tenantKey GROUP BY v.produit ORDER BY SUM(v.quantite) DESC")
    List<Object[]> findTopProduits(@Param("tenantKey") String tenantKey, Pageable pageable);

    @Query("SELECT v.produit, SUM(v.quantite), SUM(v.quantite * v.prixUnitaire) " +
            "FROM Vente v WHERE v.tenantKey = :tenantKey AND v.dateVente BETWEEN :debut AND :fin " +
            "GROUP BY v.produit ORDER BY SUM(v.quantite) DESC")
    List<Object[]> findTopProduitsParPeriode(@Param("tenantKey") String tenantKey,
                                             @Param("debut") LocalDate debut,
                                             @Param("fin") LocalDate fin,
                                             Pageable pageable);

    @Query("SELECT v.devise, COALESCE(SUM(v.quantite * v.prixUnitaire), 0) FROM Vente v WHERE v.tenantKey = :tenantKey GROUP BY v.devise")
    List<Object[]> sumByDevise(@Param("tenantKey") String tenantKey);

    @Query("SELECT YEAR(v.dateVente), MONTH(v.dateVente), v.devise, COALESCE(SUM(v.quantite * v.prixUnitaire), 0) " +
            "FROM Vente v WHERE v.tenantKey = :tenantKey " +
            "AND (:debut IS NULL OR v.dateVente >= :debut) " +
            "AND (:fin IS NULL OR v.dateVente <= :fin) " +
            "GROUP BY YEAR(v.dateVente), MONTH(v.dateVente), v.devise " +
            "ORDER BY YEAR(v.dateVente), MONTH(v.dateVente)")
    List<Object[]> sumByMonthAndDevise(@Param("tenantKey") String tenantKey,
                                       @Param("debut") LocalDate debut,
                                       @Param("fin") LocalDate fin);
}
