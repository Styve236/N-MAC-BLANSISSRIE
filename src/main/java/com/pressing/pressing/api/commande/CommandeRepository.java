package com.pressing.pressing.api.commande;

import com.pressing.pressing.api.client.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
    //1.chercher toutes les commandes d'un client
    List<Commande> findByClientIdclient(Long clientId);

    //2. chercher par le statut
    List<Commande> findByStatut(StatutCommande statut);

    //3.les commandes du jour pour le planning
    List<Commande> findByDateCreationBetween(LocalDateTime debut, LocalDateTime fin);

    //4. voir les commandes en retard
    @Query("SELECT c FROM Commande c WHERE c.dateRecuperationPrevue > :now AND c.statut <> 'PRET'")
    List<Commande> findCommandeEnRetard(@Param("now") LocalDateTime now);

    // les commandes non payées
    @Query("SELECT c FROM Commande c WHERE c.montantPaye < c.montantTotal")
    List<Commande> findByMontantPayeLessThanMontantTotal();

    //6. retrouver une commande par son ticket (reçu)
    java.util.Optional<Commande> findByNumeroTicket(String numeroTicket);
}