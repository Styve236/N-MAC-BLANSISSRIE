package com.pressing.pressing.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MILLIS = 60_000L;

    private static class Compteur {
        final long debut = System.currentTimeMillis();
        int compteur = 1;
    }

    private final Map<String, Compteur> compteurs = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = obtenirIp(request);
        long now = System.currentTimeMillis();

        Compteur c = compteurs.compute(ip, (k, v) -> {
            if (v == null) return new Compteur();
            if (now - v.debut > WINDOW_MILLIS) {
                Compteur nouveau = new Compteur();
                return nouveau;
            }
            v.compteur++;
            return v;
        });

        if (c.compteur > MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"message\":\"Trop de tentatives. Réessayez dans une minute.\"}");
            return false;
        }
        return true;
    }

    private String obtenirIp(HttpServletRequest request) {
        String forward = request.getHeader("X-Forwarded-For");
        if (forward != null && !forward.isBlank()) {
            return forward.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public void reset() {
        compteurs.clear();
    }
}
