package com.pressing.pressing.api.dto.response;
import java.math.BigDecimal;
import lombok.Data;




@Data
public class LigneRecuDTO {
    private String designation;
    private String typeNettoyage;
    private String unite;
    private Integer quantite;
    private BigDecimal poids;
    private BigDecimal montant;
}