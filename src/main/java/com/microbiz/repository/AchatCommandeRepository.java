package com.microbiz.repository;

import com.microbiz.model.AchatCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface AchatCommandeRepository extends JpaRepository<AchatCommande, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AchatCommande> findById(Long id);
}
