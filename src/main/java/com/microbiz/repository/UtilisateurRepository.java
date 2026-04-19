package com.microbiz.repository;
import com.microbiz.model.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    // Methode cle : utilisee par Spring Security
    // pour charger l utilisateur a la connexion
    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> findByEmailAndTenantKey(String email, String tenantKey);
    boolean existsByEmail(String email);
    boolean existsByEmailAndTenantKey(String email, String tenantKey);
    List<Utilisateur> findByRole(String role);
    List<Utilisateur> findByTenantKey(String tenantKey);
    List<Utilisateur> findByTenantKeyAndRole(String tenantKey, String role);
    Page<Utilisateur> findByTenantKey(String tenantKey, Pageable pageable);
    Page<Utilisateur> findByTenantKeyAndRole(String tenantKey, String role, Pageable pageable);
    Page<Utilisateur> findByTenantKeyAndNomContainingIgnoreCase(String tenantKey, String nom, Pageable pageable);
    Page<Utilisateur> findByTenantKeyAndEmailContainingIgnoreCase(String tenantKey, String email, Pageable pageable);
    Page<Utilisateur> findByTenantKeyAndRoleAndNomContainingIgnoreCase(String tenantKey, String role, String nom, Pageable pageable);
    Page<Utilisateur> findByTenantKeyAndRoleAndEmailContainingIgnoreCase(String tenantKey, String role, String email, Pageable pageable);
}
