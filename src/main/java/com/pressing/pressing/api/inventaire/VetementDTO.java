package com.pressing.pressing.api.inventaire;

import lombok.Data;

@Data
public class VetementDTO {
    private Long idvetement;
    private String libelle;
    private String typedevetement;
    private Integer quantiteStock;
}