package com.pressing.pressing.api.dto.request;
import com.pressing.pressing.api.entite.StatutLivraison;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;




@Data
public class LivraisonRequestDTO {
    private String adresseLivraison;
    private Long livreurId;
    private LocalDateTime dateLivraisonPrevue;
    private BigDecimal fraisLivraison;
    private StatutLivraison statut;
}