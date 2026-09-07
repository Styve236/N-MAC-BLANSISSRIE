package com.pressing.pressing.api.auth;

import com.pressing.pressing.api.security.JwtUtil;
import com.pressing.pressing.api.security.UtilisateurPrincipal;
import com.pressing.pressing.api.security.UtilisateurDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UtilisateurDetailsService utilisateurDetailsService;

    @PostMapping("/login")
    public ResponseEntity<ConnexionResponse> login(@Valid @RequestBody LoginRequest requete) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requete.getEmail(), requete.getPassword()));
        UtilisateurPrincipal principal = (UtilisateurPrincipal) auth.getPrincipal();

        RefreshToken rt = refreshTokenService.creer(principal);

        ConnexionResponse reponse = new ConnexionResponse();
        reponse.setToken(jwtUtil.genererToken(principal));
        reponse.setRefreshToken(rt.getToken());
        reponse.setEmail(principal.getUsername());
        reponse.setNom(principal.getUsers().getNom());
        reponse.setRole(principal.getUsers().getRole().name());
        reponse.setActif(principal.getUsers().isActif());
        reponse.setExpiresIn(86400);
        return ResponseEntity.ok(reponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequestDTO requete) {
        if (requete.getRefreshToken() == null || requete.getRefreshToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Refresh token requis"));
        }
        try {
            RefreshToken rt = refreshTokenService.validerEtRetourner(requete.getRefreshToken());
            UserDetails user = utilisateurDetailsService.loadUserByUsername(
                    jwtUtil.extraireEmail(requete.getRefreshToken()));
            UtilisateurPrincipal principal = (UtilisateurPrincipal) user;

            String newAccessToken = jwtUtil.genererToken(principal);
            RefreshToken newRefreshToken = refreshTokenService.creer(principal);
            refreshTokenService.revoquer(requete.getRefreshToken());

            ConnexionResponse reponse = new ConnexionResponse();
            reponse.setToken(newAccessToken);
            reponse.setRefreshToken(newRefreshToken.getToken());
            reponse.setEmail(principal.getUsername());
            reponse.setNom(principal.getUsers().getNom());
            reponse.setRole(principal.getUsers().getRole().name());
            reponse.setActif(principal.getUsers().isActif());
            reponse.setExpiresIn(86400);
            return ResponseEntity.ok(reponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) RefreshTokenRequestDTO requete) {
        if (requete != null && requete.getRefreshToken() != null) {
            refreshTokenService.revoquer(requete.getRefreshToken());
        }
        return ResponseEntity.ok(Map.of("message", "Déconnecté avec succès"));
    }

    @GetMapping("/profil")
    public ResponseEntity<ConnexionResponse> profil(Authentication authentication) {
        UtilisateurPrincipal principal = (UtilisateurPrincipal) authentication.getPrincipal();
        ConnexionResponse reponse = new ConnexionResponse();
        reponse.setEmail(principal.getUsername());
        reponse.setNom(principal.getUsers().getNom());
        reponse.setRole(principal.getUsers().getRole().name());
        reponse.setActif(principal.getUsers().isActif());
        return ResponseEntity.ok(reponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String mauvaisIdentifiants(BadCredentialsException ex) {
        return "Email ou mot de passe incorrect";
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String compteDesactive(DisabledException ex) {
        return "Compte desactivé. Contactez un administrateur.";
    }
}
