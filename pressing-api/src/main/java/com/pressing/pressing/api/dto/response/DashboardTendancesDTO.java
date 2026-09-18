package com.pressing.pressing.api.dto.response;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardTendancesDTO {
    private TendanceDTO caJour;
    private TendanceDTO caSemaine;
    private TendanceDTO caMois;
    private long commandesEnCours;
    private long commandesTerminees;
    private BigDecimal tauxTransformation;
}