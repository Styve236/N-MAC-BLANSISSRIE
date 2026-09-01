package com.pressing.pressing.api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey cle() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String genererToken(UtilisateurPrincipal principal) {
        Date maintenant = new Date();
        Date expirationDate = new Date(maintenant.getTime() + expiration);
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("role", principal.getUsers().getRole().name())
                .claim("nom", principal.getUsers().getNom())
                .issuedAt(maintenant)
                .expiration(expirationDate)
                .signWith(cle())
                .compact();
    }

    public String extraireEmail(String token) {
        return Jwts.parser()
                .verifyWith(cle())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean valider(String token) {
        try {
            Jwts.parser().verifyWith(cle()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}