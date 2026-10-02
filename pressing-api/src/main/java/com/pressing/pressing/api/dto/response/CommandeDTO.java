package com.pressing.pressing.api.dto.response;
import com.pressing.pressing.api.entite.StatutCommande;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;




@Data
public class CommandeDTO {
    // "idcommande" et non "id" : le frontend (CommandeDTO + fidelite.html track c.idcommande)
    // attend ce nom. Avec "id" la liste des commandes du client etait vide/illisible.
    private Long idcommande;
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
