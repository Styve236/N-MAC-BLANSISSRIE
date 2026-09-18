package com.pressing.pressing.api.dto.request;
import com.pressing.pressing.api.entite.MoyenPaiement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;




@Data
public class CommandeRequestDTO {
    private Long clientId;
    private List<LigneCommandeDTO> lignes;
    private LocalDate dateRetraitPrevue;
    // Acompte versé à la prise de la commande (optionnel, >= 0)
    private BigDecimal acompte;
    // Moyen de paiement utilisé pour l'acompte (défaut : ESPECES)
    private MoyenPaiement moyenAcompte;
    // Message personnalisé envoyé à l'agent de production dans la notification interne
    private String messageAgent;
}
