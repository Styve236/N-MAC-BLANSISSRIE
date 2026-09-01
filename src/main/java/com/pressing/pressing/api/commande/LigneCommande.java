package com.pressing.pressing.api.commande;


import com.pressing.pressing.api.inventaire.Vetements;
import com.pressing.pressing.api.photo.Photo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lignecommande")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneCommande {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idligne;
    private BigDecimal poids;
    private Integer quantite;
    private BigDecimal montant;
    private String description;

    @Enumerated(EnumType.STRING)
    private TypeNettoyage typeNettoyage;

    @ManyToOne
    @JoinColumn(name = "commande_id", nullable = false)
    @JsonIgnore
    private Commande commande;

    @ManyToOne
    @JoinColumn(name = "tarif_id")
    private Tarif tarif;

    @ManyToOne
    @JoinColumn(name = "vetement_id")
    private Vetements vetements;

    @OneToMany(mappedBy = "ligneCommande", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Photo> photos = new ArrayList<>();

    public void addPhoto(Photo photo){
        photos.add(photo);
        photo.setLigneCommande(this);
    }
}
