package com.pressing.pressing.api.security;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Empêche l'API de démarrer en production avec la clé de signature de développement,
 * connue de quiconque a lu le dépôt : un jeton d'administrateur se forge en une ligne.
 *
 * L'application refuse de démarrer si le secret est resté celui par défaut ET que
 * l'on est soit en profil "prod", soit explicitement hors mode développement
 * (APP_SECURITY_MODE=production).
 */
@Component
@Slf4j
public class SecuriteProprietesGuard implements ApplicationRunner {

    /** Valeur par defaut de jwt.secret dans application.properties. */
    static final String SECRET_DEV = "dev_secret_pressing_32_caracteres_minimum_2026";

    private final String secret;
    private final String mode;
    private final Environment environment;

    public SecuriteProprietesGuard(
            @Value("${jwt.secret}") String secret,
            @Value("${app.security.mode:development}") String mode,
            Environment environment) {
        this.secret = secret;
        this.mode = mode;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean secretParDefaut = SECRET_DEV.equals(secret);
        boolean profilProd = Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.contains("prod"));

        if (!secretParDefaut) {
            if ("development".equals(mode)) {
                log.info("Mode securite : development (cle JWT personnalisee detectee).");
            }
            return;
        }

        if (profilProd || !"development".equals(mode)) {
            throw new IllegalStateException(
                    "Cle de signature JWT encore configuree sur la valeur de developpement. "
                    + "Renseignez la variable d'environnement JWT_SECRET avant de demarrer "
                    + "(et JWT_EXPIRATION / JWT_REFRESH_EXPIRATION si besoin).");
        }
        log.warn("Cle JWT de developpement en usage : ne deployez pas cette configuration.");
    }
}
