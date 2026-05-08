package com.microbiz.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
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
                        // Administration système : réservée au propriétaire du tenant.
                        .requestMatchers("/utilisateurs/**", "/audit-logs/**", "/saas/admin/**").hasRole("ADMIN")
                        // ACC-03 : les commerciaux peuvent consulter les factures/PDF liées au tenant,
                        // sans obtenir les droits d'écriture finance (statut, encaissement, création).
                        .requestMatchers(HttpMethod.GET, "/factures", "/factures/**", "/api/v1/factures", "/api/v1/factures/**")
                        .hasAnyRole("ADMIN", "GERANT", "USER", "COMPTABLE", "COMMERCIAL")
                        // Finance/comptabilité : disponible au gérant, à l'utilisateur métier et au comptable externe.
                        .requestMatchers(
                                "/depenses/**",
                                "/factures/**",
                                "/comptabilite/ohada/**",
                                "/devises/**",
                                "/api/v1/factures/**",
                                "/api/v1/paiements/**"
                        ).hasAnyRole("ADMIN", "GERANT", "USER", "COMPTABLE")
                        // Opérations métier hors administration système.
                        .requestMatchers(
                                "/fournisseurs/**",
                                "/mouvements-stock/**",
                                "/entreprise/**",
                                "/api/v1/fournisseurs/**",
                                "/api/v1/stock-alertes/**",
                                "/api/v1/achats/**"
                        ).hasAnyRole("ADMIN", "GERANT", "USER")
                        // Vente terrain : le commercial conserve ventes/clients/produits, mais pas dépenses/utilisateurs.
                        .requestMatchers("/ventes/**", "/clients/**", "/produits/**")
                        .hasAnyRole("ADMIN", "GERANT", "USER", "COMMERCIAL")
                        .requestMatchers("/api/kpis").hasAnyRole("ADMIN", "GERANT", "USER", "COMPTABLE", "COMMERCIAL")
                        // Statistiques en lecture : gérant, comptable et commercial peuvent superviser sans administrer.
                        .requestMatchers(HttpMethod.GET, "/statistiques/**")
                        .hasAnyRole("ADMIN", "GERANT", "USER", "COMPTABLE", "COMMERCIAL")
                        .requestMatchers(HttpMethod.POST, "/statistiques/**").hasAnyRole("ADMIN", "GERANT", "USER")
                        // Dashboard connecté
                        .requestMatchers("/", "/dashboard").authenticated()
                        // Tout le reste : connexion obligatoire
                        .anyRequest().authenticated()
                )
                // Les endpoints REST /api/v1/** réutilisent l'authentification par session :
                // ils doivent donc conserver la protection CSRF comme les formulaires MVC.
                .csrf(Customizer.withDefaults())
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
