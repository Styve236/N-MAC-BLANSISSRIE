package com.pressing.pressing.api.caisse;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cloture_caisse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClotureCaisse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idcloture;

    private LocalDate date;
    private LocalDateTime dateCloture;

    private BigDecimal totalLogiciel;
    private BigDecimal totalCompte;
    private BigDecimal ecart;
    private Integer nombrePaiements;

    @Column(length = 2000)
    private String detailParMoyen;

    @Column(length = 500)
    private String observations;
}