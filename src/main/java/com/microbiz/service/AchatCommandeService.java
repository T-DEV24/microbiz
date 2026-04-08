package com.microbiz.service;

import com.microbiz.model.AchatCommande;
import com.microbiz.model.Depense;
import com.microbiz.model.MouvementStock;
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
    @Autowired private MouvementStockService mouvementStockService;

    public List<AchatCommande> findAll() {
        return achatCommandeRepository.findAll();
    }

    public AchatCommande create(AchatCommande achat) {
        validerAchat(achat);
        if (achat.getQuantiteRecue() == null || achat.getQuantiteRecue() < 0) {
            achat.setQuantiteRecue(0);
        }
        if (achat.getStatut() == null) {
            achat.setStatut(AchatCommande.StatutAchat.BROUILLON);
        }
        if (achat.getDateCommande() == null) {
            achat.setDateCommande(LocalDate.now());
        }
        return achatCommandeRepository.save(achat);
    }

    public AchatCommande receptionner(Long id) {
        return receptionnerPartielle(id, null);
    }

    public AchatCommande receptionnerPartielle(Long id, Integer quantiteRecueNow) {
        AchatCommande achat = achatCommandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande achat introuvable"));
        validerAchat(achat);

        if (achat.getStatut() == AchatCommande.StatutAchat.RECEPTIONNEE) {
            return achat;
        }
        if (achat.getStatut() == AchatCommande.StatutAchat.ANNULEE) {
            throw new RuntimeException("Une commande annulée ne peut pas être réceptionnée.");
        }

        Produit produit = produitRepository.findById(achat.getProduit().getId())
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));

        int quantiteRecue = quantiteRecueNow == null ? achat.getQuantite() : quantiteRecueNow;
        if (quantiteRecue <= 0) {
            throw new RuntimeException("La quantité réceptionnée doit être strictement positive.");
        }
        if ((achat.getQuantiteRecue() + quantiteRecue) > achat.getQuantite()) {
            throw new RuntimeException("La quantité réceptionnée dépasse la quantité commandée.");
        }

        int stock = produit.getStockActuel() != null ? produit.getStockActuel() : 0;
        produit.setStockActuel(stock + quantiteRecue);
        produitRepository.save(produit);

        int totalRecu = achat.getQuantiteRecue() + quantiteRecue;
        achat.setQuantiteRecue(totalRecu);
        if (totalRecu >= achat.getQuantite()) {
            achat.setStatut(AchatCommande.StatutAchat.RECEPTIONNEE);
            achat.setDateReception(LocalDate.now());
        } else {
            achat.setStatut(AchatCommande.StatutAchat.RECEPTION_PARTIELLE);
        }

        double montant = quantiteRecue * achat.getCoutUnitaire();
        depenseService.save(Depense.builder()
                .description("Réception achat : " + produit.getNom() + " (" + quantiteRecue + " unité(s))")
                .categorie("Achats fournisseurs")
                .montant(montant)
                .build());
        mouvementStockService.enregistrer(
                produit,
                MouvementStock.TypeMouvement.ENTREE_ACHAT,
                quantiteRecue,
                "ACHAT-" + achat.getId(),
                "Réception " + (achat.getStatut() == AchatCommande.StatutAchat.RECEPTIONNEE ? "complète" : "partielle")
        );

        return achatCommandeRepository.save(achat);
    }

    private void validerAchat(AchatCommande achat) {
        if (achat == null) {
            throw new RuntimeException("La commande d'achat est obligatoire.");
        }
        if (achat.getProduit() == null || achat.getProduit().getId() == null) {
            throw new RuntimeException("Le produit de la commande est obligatoire.");
        }
        if (achat.getQuantite() == null || achat.getQuantite() <= 0) {
            throw new RuntimeException("La quantité commandée doit être strictement positive.");
        }
        if (achat.getCoutUnitaire() == null || achat.getCoutUnitaire() <= 0) {
            throw new RuntimeException("Le coût unitaire doit être strictement positif.");
        }
    }
}
