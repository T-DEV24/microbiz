package com.microbiz.service;

import com.microbiz.model.Fournisseur;
import com.microbiz.model.PmeRole;
import com.microbiz.repository.FournisseurRepository;
import com.microbiz.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FournisseurService {

    @Autowired private FournisseurRepository fournisseurRepository;

    public Page<Fournisseur> findAll(Pageable pageable) {
        if (isFournisseurConnecte()) {
            return fournisseurConnecte(pageable);
        }
        return fournisseurRepository.findByTenantKey(TenantContext.getTenant(), pageable);
    }

    public Page<Fournisseur> rechercher(String q, Pageable pageable) {
        if (isFournisseurConnecte()) {
            return fournisseurConnecte(pageable);
        }
        String tenant = TenantContext.getTenant();
        if (q == null || q.trim().isEmpty()) {
            return fournisseurRepository.findByTenantKey(tenant, pageable);
        }
        return fournisseurRepository.findByTenantKeyAndNomContainingIgnoreCaseOrTenantKeyAndEmailContainingIgnoreCaseOrTenantKeyAndTelephoneContainingIgnoreCase(
                tenant, q.trim(), tenant, q.trim(), tenant, q.trim(), pageable);
    }

    public Fournisseur findById(Long id) {
        Fournisseur fournisseur = fournisseurRepository.findByIdAndTenantKey(id, TenantContext.getTenant())
                .orElseThrow(() -> new RuntimeException("Fournisseur introuvable"));
        if (isFournisseurConnecte() && (fournisseur.getEmail() == null || !emailConnecte().equalsIgnoreCase(fournisseur.getEmail()))) {
            throw new RuntimeException("Accès fournisseur non autorisé");
        }
        return fournisseur;
    }

    public Fournisseur save(Fournisseur fournisseur) {
        if (isFournisseurConnecte()) {
            throw new RuntimeException("Le portail fournisseur est en lecture seule.");
        }
        fournisseur.setTenantKey(TenantContext.getTenant());
        return fournisseurRepository.save(fournisseur);
    }

    public void deleteById(Long id) {
        if (isFournisseurConnecte()) {
            throw new RuntimeException("Le portail fournisseur est en lecture seule.");
        }
        Fournisseur fournisseur = findById(id);
        fournisseurRepository.delete(fournisseur);
    }

    public boolean isFournisseurConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> PmeRole.FOURNISSEUR.getAuthority().equals(a.getAuthority()));
    }

    private Page<Fournisseur> fournisseurConnecte(Pageable pageable) {
        return fournisseurRepository.findByTenantKeyAndEmailIgnoreCase(TenantContext.getTenant(), emailConnecte())
                .map(f -> new PageImpl<>(java.util.List.of(f), pageable, 1))
                .orElseGet(() -> new PageImpl<>(java.util.List.of(), pageable, 0));
    }

    private String emailConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "" : auth.getName();
    }
}
