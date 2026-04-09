package com.microbiz.security;

public final class TenantContext {
    private static final ThreadLocal<String> TENANT = ThreadLocal.withInitial(() -> "default");

    private TenantContext() {}

    public static String getTenant() {
        return TENANT.get();
    }

    public static void setTenant(String tenant) {
        TENANT.set((tenant == null || tenant.isBlank()) ? "default" : tenant.trim().toLowerCase());
    }

    public static void clear() {
        TENANT.remove();
    }
}
