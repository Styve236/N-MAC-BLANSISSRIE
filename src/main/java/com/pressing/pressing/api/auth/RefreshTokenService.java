package com.pressing.pressing.api.auth;

import com.pressing.pressing.api.security.JwtUtil;
import com.pressing.pressing.api.security.UtilisateurPrincipal;
import com.pressing.pressing.api.security.UtilisateurDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final UtilisateurDetailsService utilisateurDetailsService;

    @Transactional
    public RefreshToken creer(UtilisateurPrincipal principal) {
        String token = jwtUtil.genererRefreshToken(principal);
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .userId(principal.getUsers().getIdusers())
                .expiresAt(LocalDateTime.now().plusNanos(jwtUtil.getRefreshExpirationMillis() * 1_000_000))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken validerEtRetourner(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token introuvable"));
        if (rt.isRevoked()) {
            throw new IllegalArgumentException("Refresh token révoqué");
        }
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expiré");
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
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void revoquerTout(Long userId) {
        // optionnel : révoquer tous les refresh tokens d'un utilisateur
    }
}
