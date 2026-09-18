package com.pressing.pressing.api.dto.response;
import java.time.LocalDate;
import lombok.Data;




@Data
public class LigneInventaireDTO {
    private Long idligneinventaire;
    private Long vetementId;
    private String vetementLibelle;
    private Integer quantite;
    private LocalDate dateEntree;
    private String etat;
}