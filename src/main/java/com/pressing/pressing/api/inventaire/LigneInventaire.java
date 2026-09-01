package com.pressing.pressing.api.inventaire;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "ligneinventaire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneInventaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idligneinventaire;

    private Integer quantite;
    private LocalDate dateEntree;
    private String etat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vetement_id")
    private Vetements vetements;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventaire_id")
    private Inventaire inventaire;
}
