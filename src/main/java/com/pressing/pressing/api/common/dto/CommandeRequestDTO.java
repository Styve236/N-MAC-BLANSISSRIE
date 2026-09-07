package com.pressing.pressing.api.common.dto;

import com.pressing.pressing.api.Paiement.MoyenPaiement;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommandeRequestDTO {
    private Long clientId;
    private List<LigneCommandeDTO> lignes;
    private LocalDateTime dateRetraitPrevue;
    // Acompte versé à la prise de la commande (optionnel, >= 0)
    private BigDecimal acompte;
    // Moyen de paiement utilisé pour l'acompte (défaut : ESPECES)
    private MoyenPaiement moyenAcompte;
}
