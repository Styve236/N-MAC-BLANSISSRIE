package com.pressing.pressing.api.service;

import com.pressing.pressing.api.common.exception.RefreshTokenReutiliseException;
import com.pressing.pressing.api.entite.RefreshToken;
import com.pressing.pressing.api.repository.RefreshTokenRepository;
import com.pressing.pressing.api.security.JwtUtil;
import com.pressing.pressing.api.security.UtilisateurPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cycle de vie des refresh tokens.
 *
 * La colonne "token" ne contient plus le jeton mais son empreinte SHA-256 : un
 * dump de la base ne permet plus d'obtenir une session exploitable. La recherche
 * se fait donc par empreinte. A chaque renouvellement, l'ancien jeton est revoque
 * et remplace (rotation) : si un jeton deja revoque est represente, on considere
 * qu'il a fuite et on revoque toutes les sessions du compte.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    /** Cree un refresh token et retourne le jetent en clair, qui n'est jamais stocke. */
    @Transactional
    public String creer(UtilisateurPrincipal principal) {
        String token = jwtUtil.genererRefreshToken(principal);
        RefreshToken refreshToken = RefreshToken.builder()
                .token(empreinte(token))
                .userId(principal.getUsers().getIdusers())
                .expiresAt(LocalDateTime.now().plusNanos(jwtUtil.getRefreshExpirationMillis() * 1_000_000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Transactional
    public RefreshToken validerEtRetourner(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(empreinte(token))
                .orElseThrow(() -> new IllegalArgumentException("Refresh token introuvable"));

        if (rt.isRevoked()) {
            // Rotation : un jeton revoque ne doit jamais etre represente. S'il l'est,
            // c'est qu'il circule hors de son porteur -> on coupe toutes les sessions.
            // La revocation est demandee au controleur, dans une transaction separee :
            // lever une exception ici ferait rollback de la revoquation qu'on vient
            // d'ecrire, et la fuite ne serait jamais fermee.
            throw new RefreshTokenReutiliseException(rt.getUserId());
        }
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expire");
        }
        if (!jwtUtil.valider(token)) {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            throw new IllegalArgumentException("Refresh token JWT invalide");
        }
        return rt;
    }

    @Transactional
    public void revoquer(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        refreshTokenRepository.findByToken(empreinte(token)).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    /** Appele a la desactivation d'un compte ou a la suppression de ses sessions. */
    @Transactional
    public long revoquerToutes(Long userId) {
        return refreshTokenRepository.revoquerToutesPourUtilisateur(userId);
    }

    /**
     * Coupe toutes les sessions d'un compte apres detection d'un rejeu. Transaction
     * propre : elle doit aboutir meme si la requete echoue ensuite.
     */
    @Transactional
    public long revoquerToutesApresFuite(Long userId) {
        return refreshTokenRepository.revoquerToutesPourUtilisateur(userId);
    }

    private String empreinte(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
