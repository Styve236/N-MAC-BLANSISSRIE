package com.pressing.pressing.api.livraison;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LivraisonDTO {
    private Long idcommande;
    private String numeroTicket;
    private String clientNom;
    private String clientTelephone;
    private String adresseLivraison;
    private BigDecimal fraisLivraison;
    private LocalDateTime dateLivraisonPrevue;
    private LocalDateTime dateLivraisonReelle;
    private Long livreurId;
    private String livreurNom;
    private StatutLivraison statutLivraison;
    private BigDecimal montantTotal;
    private BigDecimal montantPaye;
    private boolean payee;
}