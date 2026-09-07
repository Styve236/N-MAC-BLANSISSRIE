package com.pressing.pressing.api.commande;

import com.pressing.pressing.api.client.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
    List<Commande> findByClientIdclient(Long clientId);

    List<Commande> findByStatut(StatutCommande statut);

    Page<Commande> findByStatut(StatutCommande statut, Pageable pageable);

    Page<Commande> findByClientIdclient(Long clientId, Pageable pageable);

    Page<Commande> findByClientIdclientAndStatut(Long clientId, StatutCommande statut, Pageable pageable);

    Page<Commande> findByDateCreationBetween(LocalDateTime debut, LocalDateTime fin, Pageable pageable);

    List<Commande> findByDateCreationBetween(LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT c FROM Commande c WHERE c.dateRecuperationPrevue > :now AND c.statut <> 'PRET'")
    List<Commande> findCommandeEnRetard(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Commande c WHERE c.montantPaye < c.montantTotal")
    List<Commande> findByMontantPayeLessThanMontantTotal();

    Optional<Commande> findByNumeroTicket(String numeroTicket);
}