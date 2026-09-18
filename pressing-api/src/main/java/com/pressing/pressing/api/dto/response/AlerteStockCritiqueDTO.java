package com.pressing.pressing.api.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlerteStockCritiqueDTO {
    private Long vetementId;
    private String libelle;
    private String typedevetement;
    private Integer quantiteStock;
    private Integer seuil;
}