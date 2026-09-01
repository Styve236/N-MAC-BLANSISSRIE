package com.pressing.pressing.api.statistique;

import com.pressing.pressing.api.commande.TypeNettoyage;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TopPrestationDTO {
    private String nomTarif;
    private String typevetement;
    private TypeNettoyage typeNettoyage;
    private long nbPrestations;
    private BigDecimal total;
}