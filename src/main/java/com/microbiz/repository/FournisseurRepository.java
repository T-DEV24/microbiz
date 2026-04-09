package com.microbiz.repository;

import com.microbiz.model.Fournisseur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {
    Page<Fournisseur> findByNomContainingIgnoreCaseOrEmailContainingIgnoreCaseOrTelephoneContainingIgnoreCase(
            String nom, String email, String telephone, Pageable pageable
    );
}
