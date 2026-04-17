package com.microbiz.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class AuthenticatedUser extends User {

    private final String tenantKey;

    public AuthenticatedUser(String username,
                             String password,
                             String tenantKey,
                             Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.tenantKey = tenantKey;
    }

    public String getTenantKey() {
        return tenantKey;
    }
}
