package com.pressing.pressing.api.dto.response;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;




@Data
public class StatutPaiementDTO {
    private Long idcommande;
    private String numeroTicket;
    private BigDecimal montantTotal;
    private BigDecimal montantPaye;
    private BigDecimal resteAPayer;
    private boolean payee;
    private List<PaiementResponseDTO> paiements;
}