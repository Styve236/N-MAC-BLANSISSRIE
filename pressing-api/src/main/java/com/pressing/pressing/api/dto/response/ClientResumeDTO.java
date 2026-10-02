package com.pressing.pressing.api.dto.response;

import lombok.Data;

/**
 * Apercu du client imbrique dans une commande.
 * Les noms de champs sont alignes sur l'entite pour ne pas casser le contrat Angular.
 */
@Data
public class ClientResumeDTO {
    private Long idclient;
    private String nom;
    private String telephone;
    private String ville;
    private String quartier;
    private Integer points_fidelites;
}
