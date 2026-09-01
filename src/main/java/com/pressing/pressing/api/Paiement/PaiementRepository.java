package com.pressing.pressing.api.Paiement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    @Query("SELECT SUM(p.montant) FROM Paiement p WHERE DATE(p.datePaiement) = :date")
    BigDecimal sumMontantByDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(p.montant) FROM Paiement p WHERE p.datePaiement BETWEEN :debut AND :fin")
    BigDecimal sumMontantBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);
}