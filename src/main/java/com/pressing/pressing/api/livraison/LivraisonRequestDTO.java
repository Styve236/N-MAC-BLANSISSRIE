package com.pressing.pressing.api.livraison;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LivraisonRequestDTO {
    private String adresseLivraison;
    private Long livreurId;
    private LocalDateTime dateLivraisonPrevue;
    private BigDecimal fraisLivraison;
    private StatutLivraison statut;
}