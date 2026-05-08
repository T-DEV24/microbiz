package com.microbiz.config;
import com.microbiz.model.*;
import com.microbiz.repository.*;
import com.microbiz.model.PmeRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired private UtilisateurRepository utilisateurRepo;
    @Autowired private ProduitRepository produitRepo;
    @Autowired private CategorieRepository categorieRepo;
    @Autowired private ClientRepository clientRepo;
    @Autowired private FournisseurRepository fournisseurRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Value("${microbiz.seed.demo-data:false}") private boolean seedDemoData;
    @Value("${microbiz.seed.admin-password:}") private String adminPassword;
    @Value("${microbiz.seed.user-password:}") private String userPassword;
    @Value("${spring.datasource.url:}") private String datasourceUrl;
    @Override
    public void run(String... args) {
        boolean embeddedLocalDatabase = isEmbeddedLocalDatabase();
        boolean shouldSeedDemoData = seedDemoData || embeddedLocalDatabase;
        if (!shouldSeedDemoData) {
            return;
        }

        String effectiveAdminPassword = normalizeSeedPassword(adminPassword, embeddedLocalDatabase ? "admin123" : "");
        String effectiveUserPassword = normalizeSeedPassword(userPassword, embeddedLocalDatabase ? "user123" : "");

        if (effectiveAdminPassword.isBlank() || effectiveUserPassword.isBlank()) {
            System.out.println("[Microbiz] Initialisation de démo ignorée : définissez MICROBIZ_ADMIN_PASSWORD et MICROBIZ_USER_PASSWORD.");
            return;
        }

        // Creer les comptes seulement si la base est vide
        if (utilisateurRepo.count() == 0) {
            Utilisateur admin = Utilisateur.builder()
                    .nom("Administrateur")
                    .email("admin@microbiz.com")
                    .motDePasse(passwordEncoder.encode(adminPassword))  // hache !
                    .role(PmeRole.ADMIN.getAuthority())
                    .tenantKey("default")
                    .build();
            utilisateurRepo.save(admin);
            Utilisateur user = Utilisateur.builder()
                    .nom("Jean Dupont")
                    .email("jean@microbiz.com")
                    .motDePasse(passwordEncoder.encode(userPassword))
                    .role(PmeRole.USER.getAuthority())
                    .tenantKey("default")
                    .build();
            utilisateurRepo.save(user);
            Utilisateur gerant = Utilisateur.builder()
                    .nom("Co-gérant")
                    .email("gerant@microbiz.com")
                    .motDePasse(passwordEncoder.encode(userPassword))
                    .role(PmeRole.GERANT.getAuthority())
                    .tenantKey("default")
                    .build();
            utilisateurRepo.save(gerant);
            Utilisateur comptable = Utilisateur.builder()
                    .nom("Comptable externe")
                    .email("comptable@microbiz.com")
                    .motDePasse(passwordEncoder.encode(userPassword))
                    .role(PmeRole.COMPTABLE.getAuthority())
                    .tenantKey("default")
                    .build();
            utilisateurRepo.save(comptable);
            Utilisateur commercial = Utilisateur.builder()
                    .nom("Awa Commercial")
                    .email("commercial@microbiz.com")
                    .motDePasse(passwordEncoder.encode(userPassword))
                    .role(PmeRole.COMMERCIAL.getAuthority())
                    .tenantKey("default")
                    .build();
            utilisateurRepo.save(commercial);
            Utilisateur fournisseurUser = Utilisateur.builder()
                    .nom("Distrib Pro")
                    .email("fournisseur@microbiz.com")
                    .motDePasse(passwordEncoder.encode(userPassword))
                    .role(PmeRole.FOURNISSEUR.getAuthority())
                    .tenantKey("default")
                    .build();
            utilisateurRepo.save(fournisseurUser);
            System.out.println("  ADMIN -> admin@microbiz.com / [mot de passe via variable d'environnement]");
            System.out.println("  USER  -> jean@microbiz.com  / [mot de passe via variable d'environnement]");
            System.out.println("  GER   -> gerant@microbiz.com / [mot de passe via variable d'environnement]");
            System.out.println("  CPT   -> comptable@microbiz.com / [mot de passe via variable d'environnement]");
            System.out.println("  COM   -> commercial@microbiz.com / [mot de passe via variable d'environnement]");
            System.out.println("  FOUR  -> fournisseur@microbiz.com / [mot de passe via variable d'environnement]");
        }
        // Produits de demo
        if (produitRepo.count() == 0) {
            if (categorieRepo.count() == 0) {
                categorieRepo.save(Categorie.builder().nom("Boissons").build());
                categorieRepo.save(Categorie.builder().nom("Alimentaire").build());
                categorieRepo.save(Categorie.builder().nom("Cosmétique").build());
                categorieRepo.save(Categorie.builder().nom("Autres").build());
            }
            produitRepo.save(Produit.builder().nom("Jus de gingembre")
                    .categorie("Boissons").prixVente(600.0)
                    .coutRevient(250.0).stockActuel(50).tenantKey("default").build());
            produitRepo.save(Produit.builder().nom("Jus de bissap")
                    .categorie("Boissons").prixVente(600.0)
                    .coutRevient(220.0).stockActuel(40).tenantKey("default").build());
            produitRepo.save(Produit.builder().nom("Jus de tamarin")
                    .categorie("Boissons").prixVente(600.0)
                    .coutRevient(230.0).stockActuel(8).tenantKey("default").build());
            produitRepo.save(Produit.builder().nom("Eau de coco")
                    .categorie("Boissons").prixVente(600.0)
                    .coutRevient(280.0).stockActuel(5).tenantKey("default").build());
        }
        // Fournisseur de demo pour le portail fournisseur
        if (fournisseurRepo.count() == 0) {
            fournisseurRepo.save(Fournisseur.builder()
                    .nom("Distrib Pro")
                    .telephone("+237 650 000 111")
                    .email("fournisseur@microbiz.com")
                    .tenantKey("default")
                    .build());
        }
        // Clients de demo
        if (clientRepo.count() == 0) {
            clientRepo.save(Client.builder().nom("Marie Ngo")
                    .telephone("+237 670 123 456").tenantKey("default").build());
            clientRepo.save(Client.builder().nom("Paul Ateba")
                    .telephone("+237 655 789 012").tenantKey("default").build());
            clientRepo.save(Client.builder().nom("Sophie Mbarga")
                    .telephone("+237 690 345 678").tenantKey("default").build());
        }
    }
    private boolean isEmbeddedLocalDatabase() {
        return datasourceUrl != null && datasourceUrl.startsWith("jdbc:h2:");
    }

    private String normalizeSeedPassword(String configuredPassword, String fallbackPassword) {
        if (configuredPassword != null && !configuredPassword.isBlank()) {
            return configuredPassword;
        }
        return fallbackPassword == null ? "" : fallbackPassword;
    }

    private String maskPasswordSource(String configuredPassword, boolean embeddedLocalDatabase, String fallbackPassword) {
        if (configuredPassword != null && !configuredPassword.isBlank()) {
            return "[mot de passe via variable d'environnement]";
        }
        if (embeddedLocalDatabase) {
            return fallbackPassword + " [démo locale H2]";
        }
        return "[non configuré]";
    }

}
