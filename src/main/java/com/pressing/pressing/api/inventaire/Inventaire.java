package com.pressing.pressing.api.inventaire;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inventaire")
@Getter
@Setter
public class Inventaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idinventaire;
    private String typevetement;
    private Integer quantite;


}
