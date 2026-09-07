package com.pressing.pressing.api.inventaire;

import lombok.Data;

@Data
public class LigneInventaireRequestDTO {
    private Long vetementId;
    private Integer quantite;
    private String etat;
}