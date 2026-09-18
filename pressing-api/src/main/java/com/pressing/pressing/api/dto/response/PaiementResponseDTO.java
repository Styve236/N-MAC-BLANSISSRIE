package com.pressing.pressing.api.dto.response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;




@Data
public class PaiementResponseDTO {
    private Long idpaiement;
    private BigDecimal montant;
    private String moyenPaiement;
    private String typePaiement;
    private String referenceTransaction;
    private LocalDateTime datePaiement;
    private Long idcommande;
    private String numeroTicket;
}