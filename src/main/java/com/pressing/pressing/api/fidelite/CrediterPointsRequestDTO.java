package com.pressing.pressing.api.fidelite;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrediterPointsRequestDTO {
    private Long clientId;
    private Long commandeId;
    private Integer nbPoints;
    private String description;
}