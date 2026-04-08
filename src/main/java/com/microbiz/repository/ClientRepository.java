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
    List<Client> findByNomContainingIgnoreCase(String nom);
    Page<Client> findByDeletedAtIsNull(Pageable pageable);
    Page<Client> findByDeletedAtIsNotNull(Pageable pageable);
    Page<Client> findByDeletedAtIsNullAndNomContainingIgnoreCase(String nom, Pageable pageable);
    Optional<Client> findByTelephone(String telephone);
    Optional<Client> findByEmail(String email);
}
