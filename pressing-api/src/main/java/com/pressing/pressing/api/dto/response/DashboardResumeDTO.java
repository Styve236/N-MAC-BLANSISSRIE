package com.pressing.pressing.api.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardResumeDTO {
    private DashboardTendancesDTO tendances;
    private List<AlerteCommandeRetardDTO> commandesEnRetard = new ArrayList<>();
    private List<AlerteClientImpayeDTO> clientsImpayes = new ArrayList<>();
    private List<AlerteStockCritiqueDTO> stockCritique = new ArrayList<>();

    // Repartition pour les deux anneaux du dashboard.
    // Les cles sont les noms des enums (RECU, PRET... / ESPECES, MTN_MONEY...)
    // car le frontend s'en sert aussi pour choisir le libelle et la couleur.
    private Map<String, Long> commandesParStatut = new LinkedHashMap<>();
    private Map<String, BigDecimal> encaissementParMoyen = new LinkedHashMap<>();
}