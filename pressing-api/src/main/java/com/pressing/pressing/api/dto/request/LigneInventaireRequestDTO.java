package com.pressing.pressing.api.dto.request;
import lombok.Data;



@Data
public class LigneInventaireRequestDTO {
    private Long vetementId;
    private Integer quantite;
    private String etat;
}