package com.pressing.pressing.api.dto.response;

import com.pressing.pressing.api.entite.StatutCommande;
import com.pressing.pressing.api.entite.StatutLivraison;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Representation d'une commande exposee par l'API.
 * L'entite JPA n'est plus serialisee directement : les champs correspondent a
 * l'ancien JSON pour rester compatible avec le frontend Angular.
 */
@Data
public class CommandeDetailDTO {
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
    private String adresseLivraison;
    private BigDecimal fraisLivraison;
    private LocalDateTime dateLivraisonPrevue;
    private LocalDateTime dateLivraisonReelle;
    private StatutLivraison statutLivraison;
    private Boolean masqueeParLivreur;
    private ClientResumeDTO client;
    private List<LigneCommandeDetailDTO> lignes = new ArrayList<>();
    private List<PaiementResumeDTO> paiements = new ArrayList<>();
}
