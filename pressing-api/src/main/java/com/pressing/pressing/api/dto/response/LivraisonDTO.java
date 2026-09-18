package com.pressing.pressing.api.dto.response;
import com.pressing.pressing.api.entite.StatutLivraison;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;




@Data
public class LivraisonDTO {
    private Long idcommande;
    private String numeroTicket;
    private String clientNom;
    private String clientTelephone;
    private String adresseLivraison;
    private BigDecimal fraisLivraison;
    private LocalDateTime dateLivraisonPrevue;
    private LocalDateTime dateLivraisonReelle;
    private Long livreurId;
    private String livreurNom;
    private StatutLivraison statutLivraison;
    private BigDecimal montantTotal;
    private BigDecimal montantPaye;
    private boolean payee;
}