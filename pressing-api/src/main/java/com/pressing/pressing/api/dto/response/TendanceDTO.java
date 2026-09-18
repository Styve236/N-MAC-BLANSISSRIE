package com.pressing.pressing.api.dto.response;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TendanceDTO {
    private BigDecimal actuel;
    private BigDecimal precedent;
    private BigDecimal variation;

    public TendanceDTO(BigDecimal actuel, BigDecimal precedent) {
        this.actuel = actuel;
        this.precedent = precedent;
        this.variation = precedent.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : actuel.subtract(precedent)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(precedent.abs(), 1, java.math.RoundingMode.HALF_UP);
    }
}
