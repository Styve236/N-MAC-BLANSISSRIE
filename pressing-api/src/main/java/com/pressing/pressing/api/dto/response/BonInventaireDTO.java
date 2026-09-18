package com.pressing.pressing.api.dto.response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;




@Data
public class BonInventaireDTO {
    private Long idinventaire;
    private LocalDate dateInventaire;
    private String observations;
    private Integer totalQuantite;
    private List<LigneInventaireDTO> lignes = new ArrayList<>();
}