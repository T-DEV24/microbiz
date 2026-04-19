package com.microbiz.repository;

import com.microbiz.model.Facture;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, Long> {
    Optional<Facture> findTopByOrderByIdDesc();
    boolean existsByNumero(String numero);
    List<Facture> findAllByTenantKeyOrderByDateEmissionDesc(String tenantKey);
    Optional<Facture> findByIdAndTenantKey(Long id, String tenantKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Facture f WHERE f.id = :id AND f.tenantKey = :tenantKey")
    Optional<Facture> findByIdAndTenantKeyForUpdate(@Param("id") Long id, @Param("tenantKey") String tenantKey);

    @Query("""
            SELECT f FROM Facture f
            WHERE f.tenantKey = :tenantKey
              AND (:q IS NULL OR :q = '' OR LOWER(f.clientNom) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:statut IS NULL OR f.statut = :statut)
              AND (:debut IS NULL OR f.dateEmission >= :debut)
              AND (:fin IS NULL OR f.dateEmission <= :fin)
            ORDER BY f.dateEmission DESC, f.id DESC
            """)
    Page<Facture> search(@Param("tenantKey") String tenantKey,
                         @Param("q") String q,
                         @Param("statut") Facture.StatutFacture statut,
                         @Param("debut") LocalDate debut,
                         @Param("fin") LocalDate fin,
                         Pageable pageable);

    List<Facture> findByTenantKeyAndStatutAndDateEcheanceBefore(String tenantKey, Facture.StatutFacture statut, LocalDate date);
    List<Facture> findByStatutAndDateEcheanceBefore(Facture.StatutFacture statut, LocalDate date);
}
