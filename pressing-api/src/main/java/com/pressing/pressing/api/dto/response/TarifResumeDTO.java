package com.pressing.pressing.api.dto.response;

import com.pressing.pressing.api.entite.TypeNettoyage;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TarifResumeDTO {
    private Long idtarif;
    private String nom;
    private String typevetement;
    private BigDecimal prixunitaire;
    private BigDecimal prixauklo;
    private TypeNettoyage typeNettoyage;
}
