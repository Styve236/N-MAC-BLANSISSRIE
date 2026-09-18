package com.pressing.pressing.api.dto.response;
import com.pressing.pressing.api.entite.StatutCommande;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;




@Data
public class CommandeDTO {
    private Long id;
    private String numeroTicket;
    private LocalDateTime dateCreation;
    private LocalDateTime dateRecuperationPrevue;
    private LocalDateTime dateRetraitReelle;
    private StatutCommande statut;
    private BigDecimal poidsTotal;
    private BigDecimal montantTotal;
    private BigDecimal montantPaye;
    private BigDecimal remise;
    private BigDecimal resteAPayer;
    private boolean payee;
}
