package com.pressing.pressing.api.dto.response;
import java.math.BigDecimal;
import lombok.Data;




@Data
public class MontantParMoyenDTO {
    private String moyen;
    private BigDecimal montant;
    private Integer nombre;
}