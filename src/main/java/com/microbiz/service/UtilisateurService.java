package com.microbiz.service;

import com.microbiz.model.Utilisateur;
import com.microbiz.repository.UtilisateurRepository;
import com.microbiz.model.PmeRole;
import com.microbiz.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

        String normalizedRole = normalizeFilterRole(role);
        if (!normalizedRole.isBlank()) {
            utilisateurs = utilisateurRepository.findByTenantKeyAndRole(tenant, normalizedRole);
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
        String normalizedRole = normalizeFilterRole(role);
        boolean hasRole = !normalizedRole.isBlank();
        boolean hasQuery = !query.isBlank();

        if (!hasRole && !hasQuery) {
            return utilisateurRepository.findByTenantKey(tenant, pageable);
        }
        if (hasRole && !hasQuery) {
            return utilisateurRepository.findByTenantKeyAndRole(tenant, normalizedRole, pageable);
        }
        if (!hasRole) {
            // approximation OR (nom/email) via fusion nom + email page pour éviter requête custom lourde
            Page<Utilisateur> byNom = utilisateurRepository.findByTenantKeyAndNomContainingIgnoreCase(tenant, query, pageable);
            if (byNom.hasContent()) return byNom;
            return utilisateurRepository.findByTenantKeyAndEmailContainingIgnoreCase(tenant, query, pageable);
        }
        Page<Utilisateur> byNom = utilisateurRepository.findByTenantKeyAndRoleAndNomContainingIgnoreCase(tenant, normalizedRole, query, pageable);
        if (byNom.hasContent()) return byNom;
        return utilisateurRepository.findByTenantKeyAndRoleAndEmailContainingIgnoreCase(tenant, normalizedRole, query, pageable);
    }

    public boolean emailExiste(String email) {
        return utilisateurRepository.existsByEmailAndTenantKey(email.trim().toLowerCase(), TenantContext.getTenant());
    }

    public Optional<Utilisateur> findById(Long id) {
        return utilisateurRepository.findById(id)
                .filter(u -> TenantContext.getTenant().equals(u.getTenantKey()));
    }

    public Utilisateur creer(String nom, String email, String motDePasse, String role) {
        Utilisateur u = Utilisateur.builder()
                .nom(nom.trim())
                .email(email.trim().toLowerCase())
                .motDePasse(passwordEncoder.encode(motDePasse))
                .role(resolveRole(role))
                .tenantKey(TenantContext.getTenant())
                .enabled(Boolean.TRUE)
                .build();
        return utilisateurRepository.save(u);
    }

    public Utilisateur modifier(Long id, String nom, String email, String role, String motDePasse, Boolean enabled) {
        Utilisateur u = findById(id).orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        String normalizedEmail = email.trim().toLowerCase();
        assertEmailAvailableForUser(normalizedEmail, id);
        String resolvedRole = resolveRole(role);
        assertAdminContinuity(u, resolvedRole, enabled == null || enabled);

        u.setNom(nom.trim());
        u.setEmail(normalizedEmail);
        u.setRole(resolvedRole);
        u.setEnabled(enabled == null || enabled);
        if (motDePasse != null && !motDePasse.isBlank()) {
            u.setMotDePasse(passwordEncoder.encode(motDePasse));
        }
        return utilisateurRepository.save(u);
    }

    public void setEnabled(Long id, boolean enabled) {
        Utilisateur u = findById(id).orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        if (!enabled) {
            assertAdminContinuity(u, u.getRole(), false);
        }
        u.setEnabled(enabled);
        utilisateurRepository.save(u);
    }

    public void supprimer(Long id) {
        Utilisateur u = findById(id).orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        assertNotCurrentUser(u);
        assertLastActiveAdminNotImpacted(u, null, false);
        utilisateurRepository.delete(u);
    }


    private void assertEmailAvailableForUser(String email, Long currentUserId) {
        utilisateurRepository.findByEmailAndTenantKey(email, TenantContext.getTenant())
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new RuntimeException("Un autre utilisateur du tenant utilise déjà cet email.");
                });
    }

    private void assertAdminContinuity(Utilisateur u, String targetRole, boolean targetEnabled) {
        assertNotCurrentUser(u);
        assertLastActiveAdminNotImpacted(u, targetRole, targetEnabled);
    }

    private void assertNotCurrentUser(Utilisateur u) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication() == null
                ? ""
                : SecurityContextHolder.getContext().getAuthentication().getName();
        if (u.getEmail() != null && u.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new RuntimeException("Un administrateur ne peut pas modifier, bloquer ou supprimer son propre compte connecté.");
        }
    }

    private void assertLastActiveAdminNotImpacted(Utilisateur u, String targetRole, boolean targetEnabled) {
        boolean currentlyActiveAdmin = PmeRole.ADMIN.matches(u.getRole()) && u.isEnabled();
        boolean remainsActiveAdmin = PmeRole.ADMIN.matches(targetRole) && targetEnabled;
        if (currentlyActiveAdmin && !remainsActiveAdmin
                && utilisateurRepository.countByTenantKeyAndRoleAndEnabledTrue(TenantContext.getTenant(), PmeRole.ADMIN.getAuthority()) <= 1) {
            throw new RuntimeException("Impossible de retirer le dernier administrateur actif du tenant.");
        }
    }

    private String resolveRole(String role) {
        return PmeRole.normalizeKnownAuthority(role);
    }

    private String normalizeFilterRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }
        return PmeRole.fromAuthority(role)
                .map(PmeRole::getAuthority)
                .orElse("");
    }
}
