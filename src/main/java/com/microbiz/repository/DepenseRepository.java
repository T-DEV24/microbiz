package com.microbiz.repository;

import com.microbiz.model.Depense;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface DepenseRepository extends JpaRepository<Depense, Long> {

    List<Depense> findAllByTenantKeyOrderByDateDepenseDesc(String tenantKey);

    List<Depense> findByTenantKeyAndDateDepenseBetweenOrderByDateDepenseDesc(String tenantKey, LocalDate debut, LocalDate fin);
    java.util.Optional<Depense> findByIdAndTenantKey(Long id, String tenantKey);

    @Query("SELECT d.devise, COALESCE(SUM(d.montant), 0) FROM Depense d WHERE d.tenantKey = :tenantKey GROUP BY d.devise")
    List<Object[]> sumByDevise(@Param("tenantKey") String tenantKey);

    @Query("SELECT d.devise, COALESCE(SUM(d.montant), 0) FROM Depense d " +
            "WHERE d.tenantKey = :tenantKey AND MONTH(d.dateDepense) = :mois AND YEAR(d.dateDepense) = :annee " +
            "GROUP BY d.devise")
    List<Object[]> sumByDeviseForMonth(@Param("tenantKey") String tenantKey, @Param("mois") int mois, @Param("annee") int annee);

    @Query("SELECT d.devise, COALESCE(SUM(d.montant), 0) FROM Depense d " +
            "WHERE d.tenantKey = :tenantKey AND d.dateDepense BETWEEN :debut AND :fin " +
            "GROUP BY d.devise")
    List<Object[]> sumByDeviseForPeriod(@Param("tenantKey") String tenantKey, @Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT d.categorie, SUM(d.montant) FROM Depense d " +
            "WHERE d.tenantKey = :tenantKey " +
            "GROUP BY d.categorie ORDER BY SUM(d.montant) DESC")
    List<Object[]> getDepensesParCategorie(@Param("tenantKey") String tenantKey);
}
