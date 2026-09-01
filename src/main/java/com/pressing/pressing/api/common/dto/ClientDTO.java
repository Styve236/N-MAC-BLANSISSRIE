package com.pressing.pressing.api.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ClientDTO {
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^[0-9+ ]{8,15}$", message = "Numéro de téléphone invalide")
    private String telephone;

    private int points_fidelites = 0;
    private String ville;
    private String quartier;
}
