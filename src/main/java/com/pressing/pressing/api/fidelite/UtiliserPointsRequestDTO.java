package com.pressing.pressing.api.fidelite;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UtiliserPointsRequestDTO {
    private Long clientId;
    private Long commandeId;
    private Integer nbPoints;
}