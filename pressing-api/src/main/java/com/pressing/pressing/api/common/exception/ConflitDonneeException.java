package com.pressing.pressing.api.common.exception;

/**
 * Operation refusee car elle contredit l'etat actuel des donnees
 * (exemple : supprimer un client qui possede deja des commandes).
 * Traduite en HTTP 409 Conflict.
 */
public class ConflitDonneeException extends RuntimeException {
    public ConflitDonneeException(String message) {
        super(message);
    }
}
