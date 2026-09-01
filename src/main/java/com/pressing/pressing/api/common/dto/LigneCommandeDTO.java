package com.pressing.pressing.api.common.dto;

import com.pressing.pressing.api.commande.TypeNettoyage;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LigneCommandeDTO {
    private Long tarifId;
    private BigDecimal poids;
    private Integer quantite;
    private TypeNettoyage typeNettoyage;
    private String description;
}
