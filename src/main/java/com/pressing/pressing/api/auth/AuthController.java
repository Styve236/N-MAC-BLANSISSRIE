package com.pressing.pressing.api.auth;

import com.pressing.pressing.api.security.JwtUtil;
import com.pressing.pressing.api.security.UtilisateurPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<ConnexionResponse> login(@Valid @RequestBody LoginRequest requete) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requete.getEmail(), requete.getPassword()));
        UtilisateurPrincipal principal = (UtilisateurPrincipal) auth.getPrincipal();

        ConnexionResponse reponse = new ConnexionResponse();
        reponse.setToken(jwtUtil.genererToken(principal));
        reponse.setEmail(principal.getUsername());
        reponse.setNom(principal.getUsers().getNom());
        reponse.setRole(principal.getUsers().getRole().name());
        reponse.setActif(principal.getUsers().isActif());
        return ResponseEntity.ok(reponse);
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
        return "Compte désactivé. Contactez un administrateur.";
    }
}