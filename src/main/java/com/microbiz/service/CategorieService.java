package com.microbiz.service;

import com.microbiz.model.Categorie;
import com.microbiz.repository.CategorieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategorieService {

    @Autowired
    private CategorieRepository categorieRepository;

    public List<Categorie> findAll() {
        return categorieRepository.findAllByOrderByNomAsc();
    }

    public String ensureExists(String nom) {
        if (nom == null || nom.isBlank()) {
            return null;
        }
        String trimmed = nom.trim();
        final String normalized = trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
        return categorieRepository.findByNomIgnoreCase(normalized)
                .map(Categorie::getNom)
                .orElseGet(() -> categorieRepository.save(Categorie.builder().nom(normalized).build()).getNom());
    }
}
