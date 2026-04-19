package com.microbiz.repository;

import com.microbiz.model.Fournisseur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {
    Page<Fournisseur> findByTenantKey(String tenantKey, Pageable pageable);
    Page<Fournisseur> findByNomContainingIgnoreCaseOrEmailContainingIgnoreCaseOrTelephoneContainingIgnoreCase(
            String nom, String email, String telephone, Pageable pageable
    );
    Page<Fournisseur> findByTenantKeyAndNomContainingIgnoreCaseOrTenantKeyAndEmailContainingIgnoreCaseOrTenantKeyAndTelephoneContainingIgnoreCase(
            String tenantNom, String nom, String tenantEmail, String email, String tenantTelephone, String telephone, Pageable pageable
    );
    java.util.Optional<Fournisseur> findByIdAndTenantKey(Long id, String tenantKey);
}
