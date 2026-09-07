package com.pressing.pressing.api.fidelite;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_point")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriquePoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idpoint;

    @Column(nullable = false)
    private Long clientId;

    private Long commandeId;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false)
    private String type;

    @Column(length = 500)
    private String description;

    @Builder.Default
    private LocalDateTime date = LocalDateTime.now();
}