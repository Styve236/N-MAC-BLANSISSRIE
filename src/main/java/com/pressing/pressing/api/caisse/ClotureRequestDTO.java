package com.pressing.pressing.api.caisse;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ClotureRequestDTO {
    private LocalDate date;
    private BigDecimal montantEnCaisse;
    private String observations;
}