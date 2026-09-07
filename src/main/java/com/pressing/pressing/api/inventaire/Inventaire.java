package com.pressing.pressing.api.inventaire;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventaire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idinventaire;

    private LocalDate dateInventaire;

    @Column(length = 500)
    private String observations;

    @OneToMany(mappedBy = "inventaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneInventaire> lignes = new ArrayList<>();
}
