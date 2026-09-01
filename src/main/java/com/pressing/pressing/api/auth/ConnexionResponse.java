package com.pressing.pressing.api.auth;

import lombok.Data;

@Data
public class ConnexionResponse {
    private String token;
    private String email;
    private String nom;
    private String role;
    private boolean actif;
}