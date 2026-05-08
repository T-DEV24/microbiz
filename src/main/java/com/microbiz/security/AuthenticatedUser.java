package com.microbiz.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class AuthenticatedUser extends User {

    private static final long serialVersionUID = 4663227312801928362L;

    private final String tenantKey;

    public AuthenticatedUser(String username,
                             String password,
                             String tenantKey,
                             Collection<? extends GrantedAuthority> authorities) {
        this(username, password, tenantKey, true, authorities);
    }

    public AuthenticatedUser(String username,
                             String password,
                             String tenantKey,
                             boolean enabled,
                             Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        this.tenantKey = tenantKey;
    }

    public String getTenantKey() {
        return tenantKey;
    }
}
