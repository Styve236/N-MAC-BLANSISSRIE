package com.pressing.pressing.api.common.exception;

import lombok.Getter;

/**
 * Un refresh token deja revoque a ete represente : il circule hors de son porteur.
 * Toutes les sessions du compte sont alors coupees.
 */
@Getter
public class RefreshTokenReutiliseException extends RuntimeException {

    private final Long userId;

    public RefreshTokenReutiliseException(Long userId) {
        super("Refresh token revoque : toutes les sessions du compte ont ete fermees");
        this.userId = userId;
    }
}
