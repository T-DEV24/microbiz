package com.microbiz.security;
import com.microbiz.model.Utilisateur;
import com.microbiz.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;
@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        // 1. Chercher l utilisateur par email dans MySQL
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Aucun utilisateur trouve avec l email : " + email
                ));
        // 2. Convertir le role en GrantedAuthority
        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority(utilisateur.getRole());
        // 3. Retourner un UserDetails que Spring Security comprend
        return new User(
                utilisateur.getEmail(),
                utilisateur.getMotDePasse(),   // deja hache en BCrypt
                Collections.singletonList(authority)
        );
    }
}