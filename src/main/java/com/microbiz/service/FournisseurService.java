package com.microbiz.service;

import com.microbiz.model.Fournisseur;
import com.microbiz.repository.FournisseurRepository;
import com.microbiz.security.TenantContext;
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
        return fournisseurRepository.findByTenantKey(TenantContext.getTenant(), pageable);
    }

    public Page<Fournisseur> rechercher(String q, Pageable pageable) {
        String tenant = TenantContext.getTenant();
        if (q == null || q.trim().isEmpty()) {
            return fournisseurRepository.findByTenantKey(tenant, pageable);
        }
        return fournisseurRepository.findByTenantKeyAndNomContainingIgnoreCaseOrTenantKeyAndEmailContainingIgnoreCaseOrTenantKeyAndTelephoneContainingIgnoreCase(
                tenant, q.trim(), tenant, q.trim(), tenant, q.trim(), pageable);
    }

    public Fournisseur findById(Long id) {
        return fournisseurRepository.findByIdAndTenantKey(id, TenantContext.getTenant())
                .orElseThrow(() -> new RuntimeException("Fournisseur introuvable"));
    }

    public Fournisseur save(Fournisseur fournisseur) {
        fournisseur.setTenantKey(TenantContext.getTenant());
        return fournisseurRepository.save(fournisseur);
    }

    public void deleteById(Long id) {
        Fournisseur fournisseur = findById(id);
        fournisseurRepository.delete(fournisseur);
    }
}
