package com.microbiz.repository;
import com.microbiz.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByTenantKeyAndNomContainingIgnoreCaseAndDeletedAtIsNull(String tenantKey, String nom);
    List<Client> findByTenantKeyAndDeletedAtIsNull(String tenantKey);
    Page<Client> findByTenantKeyAndDeletedAtIsNull(String tenantKey, Pageable pageable);
    Page<Client> findByTenantKeyAndDeletedAtIsNotNull(String tenantKey, Pageable pageable);
    Page<Client> findByTenantKeyAndDeletedAtIsNullAndNomContainingIgnoreCase(String tenantKey, String nom, Pageable pageable);
    Optional<Client> findByIdAndTenantKey(Long id, String tenantKey);
    Optional<Client> findByTenantKeyAndTelephone(String tenantKey, String telephone);
    Optional<Client> findByTenantKeyAndEmail(String tenantKey, String email);
    long countByTenantKeyAndDeletedAtIsNull(String tenantKey);
}
