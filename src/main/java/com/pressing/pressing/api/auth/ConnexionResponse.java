package com.pressing.pressing.api.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ConnexionResponse {
    private String token;
    @JsonProperty("refresh_token")
    private String refreshToken;
    private String email;
    private String nom;
    private String role;
    private boolean actif;
    @JsonProperty("expires_in")
    private long expiresIn;
}
