package com.pressing.pressing.api.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;




@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey cle() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Les durees de application.properties sont exprimees en SECONDES.
    // Date.getTime() attend des millisecondes : sans cette conversion, un token
    // configure a 86400_secondes expirait au bout de 86,4 secondes.
    private long versMillisecondes(long secondes) {
        return secondes * 1000L;
    }

    public String genererToken(UtilisateurPrincipal principal) {
        Date maintenant = new Date();
        Date expirationDate = new Date(maintenant.getTime() + versMillisecondes(expiration));
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("role", principal.getUsers().getRole().name())
                .claim("nom", principal.getUsers().getNom())
                .issuedAt(maintenant)
                .expiration(expirationDate)
                .signWith(cle())
                .compact();
    }

    public String genererRefreshToken(UtilisateurPrincipal principal) {
        Date maintenant = new Date();
        Date expirationDate = new Date(maintenant.getTime() + versMillisecondes(refreshExpiration));
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("type", "refresh")
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(maintenant)
                .expiration(expirationDate)
                .signWith(cle())
                .compact();
    }

    public long getRefreshExpirationMillis() {
        return versMillisecondes(refreshExpiration);
    }

    public long getAccessExpirationSecondes() {
        return expiration;
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

    // Seul un jeton d'ACCÈS est accepté comme Bearer. Un refresh token (type=refresh) est refusé.
    public boolean validerAccessToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(cle()).build().parseSignedClaims(token).getPayload();
            return !"refresh".equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}