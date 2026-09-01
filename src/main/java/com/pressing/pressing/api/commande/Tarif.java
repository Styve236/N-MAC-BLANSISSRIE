package com.pressing.pressing.api.commande;


import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tarif")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarif {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idtarif;

    private String typevetement;
    private BigDecimal prixauklo;
    private BigDecimal prixunitaire;
    private String nom;

    @Enumerated(EnumType.STRING)
    private TypeNettoyage typeNettoyage;

    @OneToMany(mappedBy = "tarif")
    @JsonIgnore
    private List<LigneCommande> lignes = new ArrayList<>();

    @Builder.Default
    private Boolean actif = true;
}
