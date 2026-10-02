package com.pressing.pressing.api.controller;
import com.pressing.pressing.api.dto.request.LoginRequest;
import com.pressing.pressing.api.dto.request.RefreshTokenRequestDTO;
import com.pressing.pressing.api.common.exception.RefreshTokenReutiliseException;
import com.pressing.pressing.api.dto.response.ConnexionResponse;
import com.pressing.pressing.api.security.JwtUtil;
import com.pressing.pressing.api.security.RateLimitService;
import com.pressing.pressing.api.security.UtilisateurDetailsService;
import com.pressing.pressing.api.security.UtilisateurPrincipal;
import com.pressing.pressing.api.service.RefreshTokenService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;




@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Authentification", description = "Connexion, rafraichissement et deconnexion (JWT)")
public class AuthController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UtilisateurDetailsService utilisateurDetailsService;
    private final RateLimitService rateLimitService;

    /** Verrouillage par compte : protege meme contre un brute-force reparti sur plusieurs IP. */
    private static final int MAX_TENTATIVES_PAR_COMPTE = 10;

    @PostMapping("/login")
    public ResponseEntity<ConnexionResponse> login(@Valid @RequestBody LoginRequest requete) {
        String cleCompte = "compte|" + requete.getEmail();
        if (!rateLimitService.autorise(cleCompte, MAX_TENTATIVES_PAR_COMPTE)) {
            throw new BadCredentialsException("Trop de tentatives pour ce compte");
        }

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requete.getEmail(), requete.getPassword()));
        } catch (AuthenticationException e) {
            rateLimitService.echec(cleCompte);
            throw e;
        }
        rateLimitService.reussite(cleCompte);
        UtilisateurPrincipal principal = (UtilisateurPrincipal) auth.getPrincipal();

        String refreshToken = refreshTokenService.creer(principal);

        ConnexionResponse reponse = new ConnexionResponse();
        reponse.setToken(jwtUtil.genererToken(principal));
        reponse.setRefreshToken(refreshToken);
        reponse.setEmail(principal.getUsername());
        reponse.setNom(principal.getUsers().getNom());
        reponse.setRole(principal.getUsers().getRole().name());
        reponse.setActif(principal.getUsers().isActif());
        reponse.setExpiresIn(jwtUtil.getAccessExpirationSecondes());
        return ResponseEntity.ok(reponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequestDTO requete) {
        if (requete.getRefreshToken() == null || requete.getRefreshToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Refresh token requis"));
        }
        try {
            refreshTokenService.validerEtRetourner(requete.getRefreshToken());
            UserDetails user = utilisateurDetailsService.loadUserByUsername(
                    jwtUtil.extraireEmail(requete.getRefreshToken()));
            UtilisateurPrincipal principal = (UtilisateurPrincipal) user;

            // Un compte desactivé depuis sa connexion ne doit pas pouvoir renouveler sa session.
            if (!principal.isEnabled()) {
                refreshTokenService.revoquer(requete.getRefreshToken());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Compte desactive"));
            }

            String newAccessToken = jwtUtil.genererToken(principal);
            String newRefreshToken = refreshTokenService.creer(principal);
            refreshTokenService.revoquer(requete.getRefreshToken());

            ConnexionResponse reponse = new ConnexionResponse();
            reponse.setToken(newAccessToken);
            reponse.setRefreshToken(newRefreshToken);
            reponse.setEmail(principal.getUsername());
            reponse.setNom(principal.getUsers().getNom());
            reponse.setRole(principal.getUsers().getRole().name());
            reponse.setActif(principal.getUsers().isActif());
            reponse.setExpiresIn(jwtUtil.getAccessExpirationSecondes());
            return ResponseEntity.ok(reponse);
        } catch (RefreshTokenReutiliseException e) {
            // Jeton vole : on ferme toutes les sessions du compte. Cette revocation est
            // faite ici, hors de la transaction du controle, sinon le rollback de
            // l'exception annulerait la coupure qu'on cherche a obtenir.
            long fermees = refreshTokenService.revoquerToutesApresFuite(e.getUserId());
            log.warn("Refresh token reutilise pour l'utilisateur {} : {} session(s) fermee(s)",
                    e.getUserId(), fermees);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
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
