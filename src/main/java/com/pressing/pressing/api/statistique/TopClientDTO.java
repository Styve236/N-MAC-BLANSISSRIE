package com.pressing.pressing.api.statistique;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopClientDTO {
    private String nom;
    private String telephone;
    private long nbCommandes;
    private java.math.BigDecimal totalDepense;
}