package com.pressing.pressing.api.recu;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LigneRecuDTO {
    private String designation;
    private String typeNettoyage;
    private String unite;
    private Integer quantite;
    private BigDecimal poids;
    private BigDecimal montant;
}