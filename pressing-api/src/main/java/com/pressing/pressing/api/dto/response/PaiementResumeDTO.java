package com.pressing.pressing.api.dto.response;

import com.pressing.pressing.api.entite.MoyenPaiement;
import com.pressing.pressing.api.entite.TypePaiement;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PaiementResumeDTO {
    private Long idpaiement;
    private BigDecimal montant;
    private MoyenPaiement moyenPaiement;
    private TypePaiement typePaiement;
    private String referenceTransaction;
    private LocalDateTime datePaiement;
}
