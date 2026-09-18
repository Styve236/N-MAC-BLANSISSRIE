package com.pressing.pressing.api.dto.request;
import com.pressing.pressing.api.entite.TypeNettoyage;
import java.math.BigDecimal;
import lombok.Data;




@Data
public class LigneCommandeDTO {
    private Long tarifId;
    private BigDecimal poids;
    private Integer quantite;
    private TypeNettoyage typeNettoyage;
    private String description;
}
