package com.pressing.pressing.api.dto.request;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;




@Data
public class ClotureRequestDTO {
    private LocalDate date;
    private BigDecimal montantEnCaisse;
    private String observations;
}