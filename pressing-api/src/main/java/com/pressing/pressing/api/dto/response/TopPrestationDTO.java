package com.pressing.pressing.api.dto.response;
import com.pressing.pressing.api.entite.TypeNettoyage;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;




@Getter
@Setter
public class TopPrestationDTO {
    private String nomTarif;
    private String typevetement;
    private TypeNettoyage typeNettoyage;
    private long nbPrestations;
    private BigDecimal total;
}