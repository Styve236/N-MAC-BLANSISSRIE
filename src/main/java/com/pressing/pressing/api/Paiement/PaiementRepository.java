package com.pressing.pressing.api.Paiement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    @Query("SELECT SUM(p.montant) FROM Paiement p WHERE DATE(p.datePaiement) = :date")
    BigDecimal sumMontantByDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(p.montant) FROM Paiement p WHERE p.datePaiement BETWEEN :debut AND :fin")
    BigDecimal sumMontantBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    List<Paiement> findByDatePaiementBetween(LocalDateTime debut, LocalDateTime fin);

    Page<Paiement> findAllByOrderByDatePaiementDesc(Pageable pageable);

    @Query("SELECT p FROM Paiement p JOIN p.commande c WHERE c.client.idclient = :clientId ORDER BY p.datePaiement DESC")
    Page<Paiement> findByClientIdclient(@Param("clientId") Long clientId, Pageable pageable);
}