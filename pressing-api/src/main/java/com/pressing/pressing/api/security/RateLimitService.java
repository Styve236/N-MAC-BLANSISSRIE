package com.pressing.pressing.api.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Compteurs de tentatives en memoire, partages par le limiteur d'IP (intercepteur)
 * et le limiteur par compte (AuthController).
 *
 * Les entrees expirent apres la fenetre et sont purgees periodiquement : le compteur
 * ne doit pas survivre indefiniment en memoire. En cas de deploiement sur plusieurs
 * instances, remplacer ce service par un backend partage (Redis).
 */
@Service
public class RateLimitService {

    private static final long FENETRE_DEFAUT_MILLIS = 15 * 60_000L;
    private static final int TAILLE_MAX_ENTREES = 20_000;

    private final Map<String, Compteur> compteurs = new ConcurrentHashMap<>();

    private static final class Compteur {
        private long debut = System.currentTimeMillis();
        private int valeur;

        private boolean expire(long maintenant, long fenetreMillis) {
            return maintenant - debut > fenetreMillis;
        }
    }

    /** Indique si une nouvelle tentative est encore acceptable pour cette cle. */
    public boolean autorise(String cle, int maxTentatives) {
        return autorise(cle, maxTentatives, FENETRE_DEFAUT_MILLIS);
    }

    public boolean autorise(String cle, int maxTentatives, long fenetreMillis) {
        if (cle == null) {
            return true;
        }
        Compteur c = compteurs.get(cle);
        if (c == null) {
            return true;
        }
        if (c.expire(System.currentTimeMillis(), fenetreMillis)) {
            compteurs.remove(cle, c);
            return true;
        }
        return c.valeur < maxTentatives;
    }

    /** Enregistre un echec. */
    public void echec(String cle) {
        incrementer(cle, FENETRE_DEFAUT_MILLIS);
    }

    public void echec(String cle, long fenetreMillis) {
        incrementer(cle, fenetreMillis);
    }

    /** Oublie les echecs precedents (connexion reussie). */
    public void reussite(String cle) {
        if (cle != null) {
            compteurs.remove(cle);
        }
    }

    private void incrementer(String cle, long fenetreMillis) {
        if (cle == null) {
            return;
        }
        long maintenant = System.currentTimeMillis();
        compteurs.compute(cle, (k, v) -> {
            if (v == null) {
                Compteur nouveau = new Compteur();
                nouveau.valeur = 1;
                return nouveau;
            }
            if (v.expire(maintenant, fenetreMillis)) {
                v.debut = maintenant;
                v.valeur = 1;
                return v;
            }
            v.valeur++;
            return v;
        });
        purgerSiTropGros(maintenant);
    }

    /**
     * Borne la taille du cache. Si trop d'entrees sont vivantes, on vide le cache plutot
     * que de bloquer tout le monde : la memoire prime, le limiteur n'est qu'une defense
     * de premiere ligne.
     */
    private void purgerSiTropGros(long maintenant) {
        if (compteurs.size() <= TAILLE_MAX_ENTREES) {
            return;
        }
        compteurs.entrySet().removeIf(entry -> entry.getValue().expire(maintenant, FENETRE_DEFAUT_MILLIS));
        if (compteurs.size() > TAILLE_MAX_ENTREES) {
            compteurs.clear();
        }
    }
}
