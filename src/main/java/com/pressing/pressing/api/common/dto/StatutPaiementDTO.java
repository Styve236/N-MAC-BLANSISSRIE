package com.pressing.pressing.api.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StatutPaiementDTO {
    private Long idcommande;
    private String numeroTicket;
    private BigDecimal montantTotal;
    private BigDecimal montantPaye;
    private BigDecimal resteAPayer;
    private boolean payee;
    private List<PaiementResponseDTO> paiements;
}