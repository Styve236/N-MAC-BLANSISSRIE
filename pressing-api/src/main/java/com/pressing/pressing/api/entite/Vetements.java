package com.pressing.pressing.api.entite;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pressing.pressing.api.entite.LigneCommande;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;





@Entity
@Table(name = "vetements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vetements {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idvetement;

    private String libelle;
    private String typedevetement;
    private Integer quantiteStock;

    @OneToMany(mappedBy = "vetements")
    @JsonIgnore
    private List<LigneCommande> lignes = new ArrayList<>();
}
