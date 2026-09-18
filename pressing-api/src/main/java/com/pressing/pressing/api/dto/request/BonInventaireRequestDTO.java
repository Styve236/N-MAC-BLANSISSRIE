package com.pressing.pressing.api.dto.request;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;




@Data
public class BonInventaireRequestDTO {
    private LocalDate date;
    private String observations;
    private List<LigneInventaireRequestDTO> lignes = new ArrayList<>();
}