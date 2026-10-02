package com.pressing.pressing.api.security;

import com.pressing.pressing.api.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Les refresh tokens sont desormais stockes sous forme d'empreinte SHA-256. Cette
 * verification unique efface les anciennes lignes, ou le jeton en clair etait conserve.
 * Consequence assumee : les sessions ouvertes avant la mise en production sont fermees,
 * chaque utilisateur se reconnecte une fois.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyRefreshTokenCleanup implements ApplicationRunner {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int supprimes = refreshTokenRepository.supprimerJetonsNonHaches();
        if (supprimes > 0) {
            log.warn("{} ancien(s) refresh token(s) en clair ont ete supprimes : reconnectez-vous.", supprimes);
        }
    }
}
