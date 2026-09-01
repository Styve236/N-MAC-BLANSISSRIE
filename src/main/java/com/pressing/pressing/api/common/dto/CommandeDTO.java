package com.pressing.pressing.api.common.dto;

import com.pressing.pressing.api.commande.StatutCommande;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CommandeDTO {
    private Long id;
    private String numeroTicket;
    private LocalDateTime dateCreation;
    private LocalDateTime dateRecuperationPrevue;
    private LocalDateTime dateRetraitReelle;
    private StatutCommande statut;
    private BigDecimal poidsTotal;
    private BigDecimal montantTotal;
    private BigDecimal montantPaye;
    private BigDecimal resteAPayer;
    private boolean payee;
}
