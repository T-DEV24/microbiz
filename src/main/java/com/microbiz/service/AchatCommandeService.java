package com.microbiz.service;

import com.microbiz.model.AchatCommande;
import com.microbiz.model.Depense;
import com.microbiz.model.Produit;
import com.microbiz.repository.AchatCommandeRepository;
import com.microbiz.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class AchatCommandeService {

    @Autowired private AchatCommandeRepository achatCommandeRepository;
    @Autowired private ProduitRepository produitRepository;
    @Autowired private DepenseService depenseService;

    public List<AchatCommande> findAll() {
        return achatCommandeRepository.findAll();
    }

    public AchatCommande create(AchatCommande achat) {
        if (achat.getStatut() == null) {
            achat.setStatut(AchatCommande.StatutAchat.BROUILLON);
        }
        if (achat.getDateCommande() == null) {
            achat.setDateCommande(LocalDate.now());
        }
        return achatCommandeRepository.save(achat);
    }

    public AchatCommande receptionner(Long id) {
        AchatCommande achat = achatCommandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande achat introuvable"));

        if (achat.getStatut() == AchatCommande.StatutAchat.RECEPTIONNEE) {
            return achat;
        }

        Produit produit = produitRepository.findById(achat.getProduit().getId())
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        int stock = produit.getStockActuel() != null ? produit.getStockActuel() : 0;
        produit.setStockActuel(stock + achat.getQuantite());
        produitRepository.save(produit);

        achat.setStatut(AchatCommande.StatutAchat.RECEPTIONNEE);
        achat.setDateReception(LocalDate.now());

        double montant = achat.getQuantite() * achat.getCoutUnitaire();
        depenseService.save(Depense.builder()
                .description("Réception achat : " + produit.getNom() + " (" + achat.getQuantite() + " unité(s))")
                .categorie("Achats fournisseurs")
                .montant(montant)
                .build());

        return achatCommandeRepository.save(achat);
    }
}
