package com.pressing.pressing.api.dto.response;
import com.pressing.pressing.api.entite.MoyenPaiement;
import com.pressing.pressing.api.entite.StatutCommande;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;




@Getter
@Setter
public class StatistiquesDTO {
    private BigDecimal chiffreAffairesJour;
    private BigDecimal chiffreAffairesMois;
    private BigDecimal totalEncaisse;
    private long totalCommandes;
    private long commandesAujourdhui;
    private long totalClients;
    private long commandesImpayees;
    private BigDecimal montantImpayeTotal;
    private Map<StatutCommande, Long> commandesParStatut;
    private Map<MoyenPaiement, BigDecimal> encaissementParMoyen;
    private List<TopClientDTO> topClients;
    private List<TopPrestationDTO> topPrestations;
}