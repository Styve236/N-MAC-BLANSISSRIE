package com.pressing.pressing.api.entite;
import com.pressing.pressing.api.entite.Client;
import com.pressing.pressing.api.entite.Paiement;
import com.pressing.pressing.api.entite.StatutLivraison;
import com.pressing.pressing.api.entite.Users;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;




@Entity
@Table(name = "commande")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commande {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idcommande;

    @Column(nullable = false, unique = true)
    private String numeroTicket;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
    private LocalDateTime dateRecuperationPrevue;
    private LocalDateTime dateRetraitReelle;

    @Enumerated(EnumType.STRING)
    private StatutCommande statut;

    private BigDecimal poidsTotal;
    private BigDecimal montantTotal;
    @Builder.Default
    private BigDecimal montantPaye = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal remise = BigDecimal.ZERO;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneCommande> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Paiement> paiements = new ArrayList<>();

    // ---- Livraison à domicile ----
    @Column(length = 500)
    private String adresseLivraison;

    private BigDecimal fraisLivraison;
    private LocalDateTime dateLivraisonPrevue;
    private LocalDateTime dateLivraisonReelle;

    @ManyToOne
    @JoinColumn(name = "livreur_id")
    private Users livreur;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutLivraison statutLivraison = StatutLivraison.A_LIVRER;

    // Permet au livreur de retirer une livraison terminée de son flux (l'admin la voit toujours)
    @Builder.Default
    private boolean masqueeParLivreur = false;

    // methode pour ajouter un paiement
    public void addpaiement(Paiement p){
        paiements.add(p);
        p.setCommande(this);
        this.montantPaye = (this.montantPaye != null ? this.montantPaye : BigDecimal.ZERO).add(p.getMontant());
        if (this.getResteAPayer().compareTo(BigDecimal.ZERO) <= 0){
            this.statut = StatutCommande.PAYEE;
        }
    }

    // Reste à payer : total − remise − déjà payé (la remise est déduite)
    public BigDecimal getResteAPayer(){
        BigDecimal total = this.montantTotal != null ? this.montantTotal : BigDecimal.ZERO;
        BigDecimal paye = this.montantPaye != null ? this.montantPaye : BigDecimal.ZERO;
        BigDecimal remise = this.remise != null ? this.remise : BigDecimal.ZERO;
        return total.subtract(remise).subtract(paye).max(BigDecimal.ZERO);
    }

    // Methode pour le boolean afin de savoir s'il a un solde
    public boolean aUnsolde(){
        return this.getResteAPayer().compareTo(BigDecimal.ZERO) > 0;
    }

    // methode pour savoir si une commande est entierement payee
    public boolean isPayee(){
        return this.getResteAPayer().compareTo(BigDecimal.ZERO) == 0;
    }
}