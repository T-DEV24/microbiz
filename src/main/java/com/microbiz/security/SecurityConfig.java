package com.microbiz.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private LoginRateLimitFilter loginRateLimitFilter;
    @Autowired
    private TenantFilter tenantFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        // Tout ce qui est public — login + ressources statiques
                        .requestMatchers(
                                "/login",
                                "/login-process",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/service-worker.js",
                                "/manifest.webmanifest",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()
                        // Routes ADMIN uniquement
                        .requestMatchers("/utilisateurs/**", "/audit-logs/**", "/saas/admin/**").hasRole("ADMIN")
                        // Accès commun métier (ADMIN + USER)
                        .requestMatchers(
                                "/depenses/**",
                                "/factures/**",
                                "/fournisseurs/**",
                                "/mouvements-stock/**",
                                "/comptabilite/ohada/**",
                                "/entreprise/**",
                                "/devises/**",
                                "/api/v1/factures/**",
                                "/api/v1/fournisseurs/**",
                                "/api/v1/paiements/**",
                                "/api/v1/stock-alertes/**",
                                "/api/v1/achats/**"
                        ).hasAnyRole("ADMIN", "USER")
                        // ROLE_COMMERCIAL : accès ventes/clients/produits
                        .requestMatchers("/ventes/**", "/clients/**", "/produits/**").hasAnyRole("ADMIN", "USER", "COMMERCIAL")
                        .requestMatchers("/api/kpis").hasAnyRole("ADMIN", "USER", "COMMERCIAL")
                        // ROLE_COMMERCIAL : stats en lecture seule
                        .requestMatchers(HttpMethod.GET, "/statistiques/**").hasAnyRole("ADMIN", "USER", "COMMERCIAL")
                        .requestMatchers(HttpMethod.POST, "/statistiques/**").hasAnyRole("ADMIN", "USER")
                        // Dashboard connecté
                        .requestMatchers("/", "/dashboard").authenticated()
                        // Tout le reste : connexion obligatoire
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/**"))
                .formLogin(form -> form
                        .loginPage("/login")
                        // FIX CRITIQUE : URL différente de loginPage → évite la boucle
                        .loginProcessingUrl("/login-process")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}
