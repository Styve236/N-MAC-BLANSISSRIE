package com.pressing.pressing.api.config;
import com.pressing.pressing.api.service.LivraisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Rattrape une seule fois les commandes PRET existantes sans livraison :
// elles deviennent des livraisons assignées au premier livreur actif.
@Component
@Order(2)
@RequiredArgsConstructor
public class RattrapageLivraisonRunner implements CommandLineRunner {

    private final LivraisonService livraisonService;

    @Override
    public void run(String... args) {
        int nb = livraisonService.rattraperLivraisonsPret();
        if (nb > 0) {
            System.out.println(">>> Rattrapage livraisons : " + nb + " commande(s) PRET transformee(s) en livraison.");
        }
    }
}