package com.pressing.pressing.api.dto.response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;




@Data
public class PaiementRecuDTO {
    private BigDecimal montant;
    private String moyenPaiement;
    private String typePaiement;
    private LocalDateTime datePaiement;
    private String referenceTransaction;
}