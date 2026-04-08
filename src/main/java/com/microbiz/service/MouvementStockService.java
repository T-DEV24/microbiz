package com.microbiz.service;

import com.microbiz.model.MouvementStock;
import com.microbiz.model.Produit;
import com.microbiz.repository.MouvementStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MouvementStockService {

    @Autowired private MouvementStockRepository mouvementStockRepository;

    public MouvementStock enregistrer(Produit produit,
                                      MouvementStock.TypeMouvement type,
                                      Integer quantite,
                                      String reference,
                                      String commentaire) {
        return mouvementStockRepository.save(MouvementStock.builder()
                .produit(produit)
                .type(type)
                .quantite(quantite)
                .reference(reference)
                .commentaire(commentaire)
                .build());
    }
}
