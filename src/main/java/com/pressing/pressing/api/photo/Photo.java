package com.pressing.pressing.api.photo;

import com.pressing.pressing.api.commande.LigneCommande;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "photo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idphoto;

    private String url;
    private String typephoto;
    private LocalDateTime dateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_commande_id", nullable = false)
    @JsonIgnore
    private LigneCommande ligneCommande;
}
