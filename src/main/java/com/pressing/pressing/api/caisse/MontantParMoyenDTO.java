package com.pressing.pressing.api.caisse;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MontantParMoyenDTO {
    private String moyen;
    private BigDecimal montant;
    private Integer nombre;
}