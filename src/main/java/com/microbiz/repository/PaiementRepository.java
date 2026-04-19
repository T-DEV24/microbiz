package com.microbiz.repository;

import com.microbiz.model.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    List<Paiement> findByTenantKeyAndFactureIdOrderByDateEncaissementDescIdDesc(String tenantKey, Long factureId);
}
