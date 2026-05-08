package com.microbiz.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum PmeRole {
    ADMIN("ROLE_ADMIN", "ADMIN", "Administrateur", "Propriétaire du tenant : paramétrage, utilisateurs, audit et toutes les opérations."),
    GERANT("ROLE_GERANT", "GERANT", "Gérant / co-gérant", "Supervision opérationnelle et financière sans administration système."),
    USER("ROLE_USER", "USER", "Utilisateur polyvalent", "Employé de confiance : ventes, stock, fournisseurs, factures et dépenses courantes."),
    COMPTABLE("ROLE_COMPTABLE", "COMPTABLE", "Comptable externe", "Accès finance/comptabilité : factures, dépenses, devises, rapports et OHADA, sans ventes directes."),
    COMMERCIAL("ROLE_COMMERCIAL", "COMMERCIAL", "Commercial", "Ventes terrain : ventes, clients, produits et consultation des factures/PDF, sans dépenses ni administration."),
    FOURNISSEUR("ROLE_FOURNISSEUR", "FOURNISSEUR", "Fournisseur", "Portail fournisseur : consultation limitée à sa fiche fournisseur et à ses informations de contact.");

    private final String authority;
    private final String securityName;
    private final String label;
    private final String description;

    PmeRole(String authority, String securityName, String label, String description) {
        this.authority = authority;
        this.securityName = securityName;
        this.label = label;
        this.description = description;
    }

    public String getAuthority() {
        return authority;
    }

    public String getSecurityName() {
        return securityName;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean matches(String role) {
        return fromAuthority(role)
                .map(this::equals)
                .orElse(false);
    }

    public static List<PmeRole> assignableRoles() {
        return List.of(USER, GERANT, COMPTABLE, COMMERCIAL, FOURNISSEUR, ADMIN);
    }

    public static Optional<PmeRole> fromAuthority(String role) {
        String normalized = normalizeAuthority(role);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(r -> r.authority.equals(normalized))
                .findFirst();
    }

    public static String normalizeAuthority(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }
        String normalized = role.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        return normalized;
    }

    public static String normalizeKnownAuthority(String role) {
        return fromAuthority(role)
                .map(PmeRole::getAuthority)
                .orElse(USER.authority);
    }

    public static String[] securityNames(PmeRole... roles) {
        return Arrays.stream(roles)
                .map(PmeRole::getSecurityName)
                .toArray(String[]::new);
    }
}
