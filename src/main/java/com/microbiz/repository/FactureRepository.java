package com.microbiz.repository;

import com.microbiz.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, Long> {
    Optional<Facture> findTopByOrderByIdDesc();
    boolean existsByNumero(String numero);
}
