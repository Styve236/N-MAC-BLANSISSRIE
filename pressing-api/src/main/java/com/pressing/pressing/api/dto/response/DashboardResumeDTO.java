package com.pressing.pressing.api.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardResumeDTO {
    private DashboardTendancesDTO tendances;
    private List<AlerteCommandeRetardDTO> commandesEnRetard = new ArrayList<>();
    private List<AlerteClientImpayeDTO> clientsImpayes = new ArrayList<>();
    private List<AlerteStockCritiqueDTO> stockCritique = new ArrayList<>();
}