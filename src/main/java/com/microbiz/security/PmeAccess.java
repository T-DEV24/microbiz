package com.microbiz.security;

import com.microbiz.model.PmeRole;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component("pmeAccess")
public class PmeAccess {

    public static final String[] SYSTEM_ADMIN = PmeRole.securityNames(PmeRole.ADMIN);
    public static final String[] INVOICE_READ = PmeRole.securityNames(PmeRole.ADMIN, PmeRole.GERANT, PmeRole.USER, PmeRole.COMPTABLE, PmeRole.COMMERCIAL);
    public static final String[] FINANCE_WRITE = PmeRole.securityNames(PmeRole.ADMIN, PmeRole.GERANT, PmeRole.USER, PmeRole.COMPTABLE);
    public static final String[] OPERATIONS = PmeRole.securityNames(PmeRole.ADMIN, PmeRole.GERANT, PmeRole.USER);
    public static final String[] SALES = PmeRole.securityNames(PmeRole.ADMIN, PmeRole.GERANT, PmeRole.USER, PmeRole.COMMERCIAL);
    public static final String[] KPI_READ = PmeRole.securityNames(PmeRole.ADMIN, PmeRole.GERANT, PmeRole.USER, PmeRole.COMPTABLE, PmeRole.COMMERCIAL);
    public static final String[] STATS_WRITE = PmeRole.securityNames(PmeRole.ADMIN, PmeRole.GERANT, PmeRole.USER);

    public boolean canManageSystem(Authentication authentication) {
        return hasAny(authentication, PmeRole.ADMIN);
    }

    public boolean canAccessSales(Authentication authentication) {
        return hasAny(authentication, PmeRole.ADMIN, PmeRole.GERANT, PmeRole.USER, PmeRole.COMMERCIAL);
    }

    public boolean canAccessOperations(Authentication authentication) {
        return hasAny(authentication, PmeRole.ADMIN, PmeRole.GERANT, PmeRole.USER);
    }

    public boolean canReadInvoices(Authentication authentication) {
        return hasAny(authentication, PmeRole.ADMIN, PmeRole.GERANT, PmeRole.USER, PmeRole.COMPTABLE, PmeRole.COMMERCIAL);
    }

    public boolean canAccessFinance(Authentication authentication) {
        return hasAny(authentication, PmeRole.ADMIN, PmeRole.GERANT, PmeRole.USER, PmeRole.COMPTABLE);
    }

    public boolean canWriteFinance(Authentication authentication) {
        return canAccessFinance(authentication);
    }

    public boolean canReadStats(Authentication authentication) {
        return hasAny(authentication, PmeRole.ADMIN, PmeRole.GERANT, PmeRole.USER, PmeRole.COMPTABLE, PmeRole.COMMERCIAL);
    }

    public boolean hasAny(Authentication authentication, PmeRole... roles) {
        if (authentication == null || roles == null || roles.length == 0) {
            return false;
        }
        Set<String> allowed = Arrays.stream(roles)
                .map(PmeRole::getAuthority)
                .collect(Collectors.toSet());
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> allowed.contains(authority.getAuthority()));
    }
}
