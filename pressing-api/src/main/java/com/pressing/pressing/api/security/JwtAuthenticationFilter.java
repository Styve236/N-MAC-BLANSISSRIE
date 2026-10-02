package com.pressing.pressing.api.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;




@Component
@RequiredArgsConstructor
public class  JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UtilisateurDetailsService utilisateurDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7);
            try {
                if (jwtUtil.validerAccessToken(token)) {
                    String email = jwtUtil.extraireEmail(token);
                    UserDetails utilisateur = utilisateurDetailsService.loadUserByUsername(email);
                    // Un compte desactive ou verrouille doit perdre l'acces immediatement,
                    // meme si son jeton n'est pas encore expire. Sans cette verification,
                    // un employe licencie conservait l'acces jusqu'a 24 h (et 30 jours via /refresh).
                    if (!utilisateur.isEnabled() || !utilisateur.isAccountNonLocked()) {
                        throw new BadCredentialsException("Compte desactive");
                    }
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            utilisateur, null, utilisateur.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignore) {
                // jeton invalide, expire, compte desactive ou introuvable : on laisse l'utilisateur non authentifie
            }
        }
        filterChain.doFilter(request, response);
    }
}