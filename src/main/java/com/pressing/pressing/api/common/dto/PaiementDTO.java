package com.pressing.pressing.api.common.dto;

import com.pressing.pressing.api.Paiement.MoyenPaiement;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaiementDTO {
    private BigDecimal montant;
    private MoyenPaiement moyenPaiement;
    private String referenceTransaction;
}
