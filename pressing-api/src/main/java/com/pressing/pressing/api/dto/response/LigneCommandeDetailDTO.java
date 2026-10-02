package com.pressing.pressing.api.dto.response;

import com.pressing.pressing.api.entite.TypeNettoyage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class LigneCommandeDetailDTO {
    private Long idligne;
    private BigDecimal poids;
    private Integer quantite;
    private BigDecimal montant;
    private String description;
    private TypeNettoyage typeNettoyage;
    private TarifResumeDTO tarif;
    private List<PhotoDTO> photos = new ArrayList<>();
}
