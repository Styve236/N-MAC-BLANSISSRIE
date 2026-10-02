package com.pressing.pressing.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Limite les tentatives sur les chemins sensibles : connexion, renouvellement de jeton
 * et ouverture d'un recu.
 *
 * Deux corrections de securite par rapport a la version precedente :
 *  - l'IP n'est plus lue dans X-Forwarded-For (en-tete forgeable par le client, qui
 *    rendait le limiteur inoperant). L'en-tete n'est pris en compte que si un reverse
 *    proxy de confiance est explicitement declare (app.rate-limit.proxy-de-confiance).
 *  - seules les requetes en echec sont comptees : une connexion reussie remet le
 *    compteur a zero, un utilisateur legitime ne peut plus se bloquer lui-meme.
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_TENTATIVES_LOGIN = 5;
    private static final int MAX_TENTATIVES_REFRESH = 10;
    private static final int MAX_VUES_RECU = 30;
    private static final long FENETRE_MILLIS = 15 * 60_000L;

    private final RateLimitService rateLimitService;

    @Value("${app.rate-limit.proxy-de-confiance:false}")
    private boolean proxyDeConfiance;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String cle = cle(request);
        if (cle == null) {
            return true;
        }
        if (rateLimitService.autorise(cle, maxTentatives(request), FENETRE_MILLIS)) {
            return true;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":429,\"message\":\"Trop de tentatives. Reessayez dans quelques minutes.\"}");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String cle = cle(request);
        if (cle == null) {
            return;
        }
        int statut = response.getStatus();
        boolean succes = statut >= 200 && statut < 300;

        // Un recu est une page client : un 404 est une tentative de devinette.
        if (estCheminRecu(request) || !succes) {
            rateLimitService.echec(cle, FENETRE_MILLIS);
        } else {
            rateLimitService.reussite(cle);
        }
    }

    private int maxTentatives(HttpServletRequest request) {
        if (estCheminRecu(request)) {
            return MAX_VUES_RECU;
        }
        return request.getRequestURI().endsWith("/login") ? MAX_TENTATIVES_LOGIN : MAX_TENTATIVES_REFRESH;
    }

    private boolean estCheminRecu(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/recu/");
    }

    /** Les chemins limites ont tous la meme cle de base : IP + type de chemin. */
    private String cle(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefixe;
        if (uri.startsWith("/api/auth/login")) {
            prefixe = "login";
        } else if (uri.startsWith("/api/auth/refresh")) {
            prefixe = "refresh";
        } else if (uri.startsWith("/recu/")) {
            prefixe = "recu";
        } else {
            return null;
        }
        return prefixe + "|" + obtenirIp(request);
    }

    private String obtenirIp(HttpServletRequest request) {
        if (proxyDeConfiance) {
            String forward = request.getHeader("X-Forwarded-For");
            if (forward != null && !forward.isBlank()) {
                return forward.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
