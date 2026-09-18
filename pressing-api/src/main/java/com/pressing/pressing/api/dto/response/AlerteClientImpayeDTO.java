package com.pressing.pressing.api.dto.response;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlerteClientImpayeDTO {
    private Long clientId;
    private String nom;
    private String telephone;
    private BigDecimal montantImpaye;
    private long nbCommandesImpayees;
}