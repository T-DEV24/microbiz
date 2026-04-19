package com.microbiz.service;

import com.microbiz.model.Utilisateur;
import com.microbiz.repository.UtilisateurRepository;
import com.microbiz.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UtilisateurService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Utilisateur> rechercher(String q, String role) {
        String tenant = TenantContext.getTenant();
        List<Utilisateur> utilisateurs;

        if (role != null && !role.isBlank()) {
            utilisateurs = utilisateurRepository.findByTenantKeyAndRole(tenant, role);
        } else {
            utilisateurs = utilisateurRepository.findByTenantKey(tenant);
        }

        if (q == null || q.isBlank()) {
            return utilisateurs;
        }

        String query = q.trim().toLowerCase();
        return utilisateurs.stream()
                .filter(u -> (u.getNom() != null && u.getNom().toLowerCase().contains(query))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(query)))
                .toList();
    }

    public Page<Utilisateur> rechercherPage(String q, String role, Pageable pageable) {
        String tenant = TenantContext.getTenant();
        String query = q == null ? "" : q.trim();
        boolean hasRole = role != null && !role.isBlank();
        boolean hasQuery = !query.isBlank();

        if (!hasRole && !hasQuery) {
            return utilisateurRepository.findByTenantKey(tenant, pageable);
        }
        if (hasRole && !hasQuery) {
            return utilisateurRepository.findByTenantKeyAndRole(tenant, role, pageable);
        }
        if (!hasRole) {
            // approximation OR (nom/email) via fusion nom + email page pour éviter requête custom lourde
            Page<Utilisateur> byNom = utilisateurRepository.findByTenantKeyAndNomContainingIgnoreCase(tenant, query, pageable);
            if (byNom.hasContent()) return byNom;
            return utilisateurRepository.findByTenantKeyAndEmailContainingIgnoreCase(tenant, query, pageable);
        }
        Page<Utilisateur> byNom = utilisateurRepository.findByTenantKeyAndRoleAndNomContainingIgnoreCase(tenant, role, query, pageable);
        if (byNom.hasContent()) return byNom;
        return utilisateurRepository.findByTenantKeyAndRoleAndEmailContainingIgnoreCase(tenant, role, query, pageable);
    }

    public boolean emailExiste(String email) {
        return utilisateurRepository.existsByEmailAndTenantKey(email.trim().toLowerCase(), TenantContext.getTenant());
    }

    public Utilisateur creer(String nom, String email, String motDePasse, String role) {
        Utilisateur u = Utilisateur.builder()
                .nom(nom.trim())
                .email(email.trim().toLowerCase())
                .motDePasse(passwordEncoder.encode(motDePasse))
                .role(resolveRole(role))
                .tenantKey(TenantContext.getTenant())
                .build();
        return utilisateurRepository.save(u);
    }

    private String resolveRole(String role) {
        if ("ROLE_ADMIN".equals(role)) return "ROLE_ADMIN";
        if ("ROLE_COMMERCIAL".equals(role)) return "ROLE_COMMERCIAL";
        return "ROLE_USER";
    }
}
