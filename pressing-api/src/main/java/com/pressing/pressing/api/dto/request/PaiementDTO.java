package com.pressing.pressing.api.dto.request;
import com.pressing.pressing.api.entite.MoyenPaiement;
import java.math.BigDecimal;
import lombok.Data;




@Data
public class PaiementDTO {
    private BigDecimal montant;
    private MoyenPaiement moyenPaiement;
    private String referenceTransaction;
}
