package com.pressing.pressing.api.client;


import com.pressing.pressing.api.commande.Commande;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "client")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long idclient
            ;
    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private  String telephone;

    private String ville;
    private String quartier;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Commande> commandes =  new ArrayList<>();

    @Builder.Default
    private int points_fidelites = 0;


}
