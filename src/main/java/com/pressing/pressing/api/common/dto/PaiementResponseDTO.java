package com.pressing.pressing.api.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaiementResponseDTO {
    private Long idpaiement;
    private BigDecimal montant;
    private String moyenPaiement;
    private String referenceTransaction;
    private LocalDateTime datePaiement;
    private Long idcommande;
    private String numeroTicket;
}