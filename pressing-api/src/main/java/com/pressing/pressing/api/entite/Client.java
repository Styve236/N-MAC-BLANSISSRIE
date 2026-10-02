package com.pressing.pressing.api.entite;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pressing.pressing.api.entite.Commande;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;





@Entity
@Table(name = "client", indexes = {
        @Index(name = "idx_client_nom", columnList = "nom"),
        @Index(name = "idx_client_quartier", columnList = "quartier")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long idclient;
    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private  String telephone;

    private String ville;
    private String quartier;

    // Pas de CascadeType.ALL : supprimer un client ne doit jamais supprimer
    // son historique de commandes (la suppression est bloquée si des commandes existent).
    @OneToMany(mappedBy = "client")
    @JsonIgnore
    private List<Commande> commandes =  new ArrayList<>();

    @Builder.Default
    private Integer points_fidelites = 0;


}
