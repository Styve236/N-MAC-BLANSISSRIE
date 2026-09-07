package com.pressing.pressing.api.inventaire;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class BonInventaireRequestDTO {
    private LocalDate date;
    private String observations;
    private List<LigneInventaireRequestDTO> lignes = new ArrayList<>();
}