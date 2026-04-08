package com.microbiz.repository;
import com.microbiz.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByNomContainingIgnoreCase(String nom);
    Optional<Client> findByTelephone(String telephone);
    Optional<Client> findByEmail(String email);
}

