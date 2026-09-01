package com.pressing.pressing.api.commande;

import com.pressing.pressing.api.Paiement.Paiement;
import com.pressing.pressing.api.client.Client;
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

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneCommande> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Paiement> paiements = new ArrayList<>();

    // methode pour ajouter un paiement
    public void addpaiement(Paiement p){
        paiements.add(p);
        p.setCommande(this);
        this.montantPaye = this.montantPaye.add(p.getMontant());
        if (this.montantPaye.compareTo(this.montantTotal) >= 0){
            this.statut = StatutCommande.PAYEE;
        }
    }

    // Methode pour le boolean afin de savoir s'il a un solde
    public boolean aUnsolde(){
        return this.montantPaye.compareTo(this.montantTotal) < 0;
    }

    // methode pour savoir si une commande est entierement payee
    public boolean isPayee(){
        return this.montantPaye.compareTo(this.montantTotal) >= 0;
    }
}