package com.pressing.pressing.api.recu;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaiementRecuDTO {
    private BigDecimal montant;
    private String moyenPaiement;
    private LocalDateTime datePaiement;
    private String referenceTransaction;
}