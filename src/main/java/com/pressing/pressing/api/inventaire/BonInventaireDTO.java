package com.pressing.pressing.api.inventaire;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class BonInventaireDTO {
    private Long idinventaire;
    private LocalDate dateInventaire;
    private String observations;
    private Integer totalQuantite;
    private List<LigneInventaireDTO> lignes = new ArrayList<>();
}