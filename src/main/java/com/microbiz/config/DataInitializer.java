package com.microbiz.config;
import com.microbiz.model.*;
import com.microbiz.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired private UtilisateurRepository utilisateurRepo;
    @Autowired private ProduitRepository produitRepo;
    @Autowired private ClientRepository clientRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Override
    public void run(String... args) {
        // Creer les comptes seulement si la base est vide
        if (utilisateurRepo.count() == 0) {
            Utilisateur admin = Utilisateur.builder()
                    .nom("Administrateur")
                    .email("admin@microbiz.com")
                    .motDePasse(passwordEncoder.encode("admin123"))  // hache !
                    .role("ROLE_ADMIN")
                    .build();
            utilisateurRepo.save(admin);
            Utilisateur user = Utilisateur.builder()
                    .nom("Jean Dupont")
                    .email("jean@microbiz.com")
                    .motDePasse(passwordEncoder.encode("user123"))
                    .role("ROLE_USER")
                    .build();
            utilisateurRepo.save(user);
            System.out.println("  ADMIN -> admin@microbiz.com / admin123");
            System.out.println("  USER  -> jean@microbiz.com  / user123");
        }
        // Produits de demo
        if (produitRepo.count() == 0) {
            produitRepo.save(Produit.builder().nom("Jus de gingembre")
                    .categorie("Boissons").prixVente(600.0)
                    .coutRevient(250.0).stockActuel(50).build());
            produitRepo.save(Produit.builder().nom("Jus de bissap")
                    .categorie("Boissons").prixVente(600.0)
                    .coutRevient(220.0).stockActuel(40).build());
            produitRepo.save(Produit.builder().nom("Jus de tamarin")
                    .categorie("Boissons").prixVente(600.0)
                    .coutRevient(230.0).stockActuel(8).build());
            produitRepo.save(Produit.builder().nom("Eau de coco")
                    .categorie("Boissons").prixVente(600.0)
                    .coutRevient(280.0).stockActuel(5).build());
        }
        // Clients de demo
        if (clientRepo.count() == 0) {
            clientRepo.save(Client.builder().nom("Marie Ngo")
                    .telephone("+237 670 123 456").build());
            clientRepo.save(Client.builder().nom("Paul Ateba")
                    .telephone("+237 655 789 012").build());
            clientRepo.save(Client.builder().nom("Sophie Mbarga")
                    .telephone("+237 690 345 678").build());
        }
    }
}