package com.microbiz.service;

import com.microbiz.model.Fournisseur;
import com.microbiz.repository.FournisseurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FournisseurService {

    @Autowired private FournisseurRepository fournisseurRepository;

    public Page<Fournisseur> findAll(Pageable pageable) {
        return fournisseurRepository.findAll(pageable);
    }

    public Page<Fournisseur> rechercher(String q, Pageable pageable) {
        if (q == null || q.trim().isEmpty()) {
            return fournisseurRepository.findAll(pageable);
        }
        return fournisseurRepository.findByNomContainingIgnoreCaseOrEmailContainingIgnoreCaseOrTelephoneContainingIgnoreCase(
                q.trim(), q.trim(), q.trim(), pageable);
    }

    public Fournisseur findById(Long id) {
        return fournisseurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fournisseur introuvable"));
    }

    public Fournisseur save(Fournisseur fournisseur) {
        return fournisseurRepository.save(fournisseur);
    }

    public void deleteById(Long id) {
        fournisseurRepository.deleteById(id);
    }
}
