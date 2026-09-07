package com.pressing.pressing.api.inventaire;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LigneInventaireDTO {
    private Long idligneinventaire;
    private Long vetementId;
    private String vetementLibelle;
    private Integer quantite;
    private LocalDate dateEntree;
    private String etat;
}