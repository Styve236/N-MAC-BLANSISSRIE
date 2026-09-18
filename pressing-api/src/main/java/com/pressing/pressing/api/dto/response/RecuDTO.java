package com.pressing.pressing.api.dto.response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;




@Data
public class RecuDTO {
    private Long idcommande;
    private String numeroTicket;
    private String lien;
    private String statut;
    private String nomClient;
    private String telephoneClient;
    private LocalDateTime dateCreation;
    private LocalDateTime dateRecuperationPrevue;
    private BigDecimal poidsTotal;
    private BigDecimal montantTotal;
    private BigDecimal remise;
    private BigDecimal montantPaye;
    private BigDecimal resteAPayer;
    private List<LigneRecuDTO> lignes;
    private List<PaiementRecuDTO> paiements;
}