package com.pressing.pressing.api.auth;

import lombok.Data;

@Data
public class RefreshTokenRequestDTO {
    private String refreshToken;
}
