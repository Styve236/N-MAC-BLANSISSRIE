package com.pressing.pressing.api.dto.response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * Vue publique et restreinte d'une commande pour le portail de suivi client.
 * Le telephone est volontairement masque : la page est accessible sans
 * authentification, un simple numero de ticket ne doit pas reveler de donnee
 * personnelle exploitable.
 */
@Data
public class SuiviDTO {
    private String numeroTicket;
    private String statut;
    private String nomClient;
    private String telephoneMasque;
    private LocalDateTime dateCreation;
    private LocalDateTime dateRecuperationPrevue;
    private BigDecimal poidsTotal;
    private BigDecimal montantTotal;
    private BigDecimal montantPaye;
    private BigDecimal resteAPayer;
    private List<LigneRecuDTO> lignes;
}
