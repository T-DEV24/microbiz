package com.microbiz.repository;

import com.microbiz.model.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategorieRepository extends JpaRepository<Categorie, Long> {
    List<Categorie> findAllByOrderByNomAsc();
    Optional<Categorie> findByNomIgnoreCase(String nom);
    boolean existsByNomIgnoreCase(String nom);
}
