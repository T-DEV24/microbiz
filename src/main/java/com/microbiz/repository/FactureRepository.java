package com.microbiz.repository;

import com.microbiz.model.Facture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, Long> {
    Optional<Facture> findTopByOrderByIdDesc();
    boolean existsByNumero(String numero);

    @Query("""
            SELECT f FROM Facture f
            WHERE (:q IS NULL OR :q = '' OR LOWER(f.clientNom) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:statut IS NULL OR f.statut = :statut)
              AND (:debut IS NULL OR f.dateEmission >= :debut)
              AND (:fin IS NULL OR f.dateEmission <= :fin)
            ORDER BY f.dateEmission DESC, f.id DESC
            """)
    Page<Facture> search(@Param("q") String q,
                         @Param("statut") Facture.StatutFacture statut,
                         @Param("debut") LocalDate debut,
                         @Param("fin") LocalDate fin,
                         Pageable pageable);
}
