# Audit de sécurité — N-MAC Blanchisserie

**Date :** 29 septembre 2026
**Périmètre :** `pressing-api` (Spring Boot) et `frontend` (Angular)
**État :** 10 points traités, 2 bugs hors audit découverts pendant la validation

---

## 1. Synthèse

| Sévérité | Points | Statut |
|---|---|---|
| Critique (P0) | 4 | 4 corrigés |
| Important (P1) | 6 | 5 corrigés, 1 partiellement |
| Hors audit | 2 | 2 corrigés |

La section 5 liste les risques qui **restent ouverts** et nécessitent une décision métier.

---

## 2. Points critiques (P0)

### 2.1 Secret JWT par défaut et initialiseur actif en production

**Risque.** Un secret JWT codé en dur dans `application.properties`, combiné à un `DataInitializer` actif, permet à quiconque connaissant le dépôt de forger un jeton administrateur sur une instance qui n'a pas été configurée.

**Correctif.**
- `SecuriteProprietesGuard` refuse le démarrage si le mode n'est pas `development`, ou si un profil `prod` est actif, tant que `JWT_SECRET` n'est pas défini.
- `app.initializer.actif` passe à `false` par défaut.

```properties
jwt.secret=${JWT_SECRET:dev_secret_pressing_32_caracteres_minimum_2026}
app.security.mode=${APP_SECURITY_MODE:development}
app.initializer.actif=${APP_INITIALIZER_ACTIF:false}
```

**Limite assumée.** En développement, la clé par défaut est tolérée pour ne pas casser l'existant. Le passage en production impose donc de poser `APP_SECURITY_MODE=production` — un oubli reste possible.

---

### 2.2 Refresh tokens stockés en clair

**Risque.** Une fuite de la base (dump, sauvegarde, injection SQL) donnait des sessions actives directement exploitables.

**Correctif.** Seul le SHA-256 du token est persisté. La rotation est conservée. `LegacyRefreshTokenCleanup` supprime au démarrage les lignes dont la longueur n'est pas 64, ce qui a purgé **470 jetons en clair** et invalidé les sessions correspondantes — comportement attendu.

---

### 2.3 Session maintenue après désactivation d'un compte

**Risque.** Un compte désactivé continuait d'utiliser son access token et de renouveler sa session.

**Correctif.** `JwtAuthenticationFilter` vérifie `isEnabled()` et `isAccountNonLocked()`. `/api/auth/refresh` refuse un compte désactivé. `UserService.changerStatut(..., false)` et la réinitialisation de mot de passe révoquent toutes les sessions.

**Vérifié.** Ancien access token `401`, refresh `401`, nouvelle connexion `403`.

---

### 2.4 Limiteur contournable via `X-Forwarded-For`

**Risque.** L'IP client était lue dans un en-tête fourni par le client. Changer d'en-tête suffisait à réinitialiser le compteur et à permettre une attaque par force brute sans limite.

**Correctif.** L'en-tête n'est lu que si un reverse proxy est explicitement déclaré :

```properties
server.forward-headers-strategy=${FORWARD_HEADERS_STRATEGY:none}
app.rate-limit.proxy-de-confiance=${RATE_LIMIT_PROXY_CONFIANCE:false}
```

`RateLimitInterceptor.java:95-103` retombe sur `getRemoteAddr()` par défaut.

**Limite assumée.** Voir section 5.3 : l'activation n'est pas sûre sans filtrage des proxies de confiance.

---

## 3. Points importants (P1)

### 3.1 Photos servies en public

`/api/photos/*/fichier` a été retiré de `permitAll`. Le frontend charge désormais un blob authentifié et révoque les `objectURL` au destroy.

**Vérifié.** Sans jeton `401`, avec jeton `200`.

### 3.2 Annuaire clients ouvert aux agents et livreurs

Restreint à `ADMIN` / `RECEPTIONNISTE` : annuaire, fiche, historique, paiements client et paiements commande.

| Endpoint | admin | recep | agent | livreur |
|---|---|---|---|---|
| `/api/clients` | 200 | 200 | 403 | 403 |
| `/api/clients/{id}` | 200 | 200 | 403 | 403 |
| `/api/commandes/client/{id}` | 200 | 200 | 403 | 403 |
| `/api/clients/{id}/paiements` | 200 | 200 | 403 | 403 |
| `/api/commandes/{id}/paiements` | 200 | 200 | 403 | 403 |
| `/api/dashboard/resume` | 200 | 200 | 403 | 403 |
| `/api/statistiques` | 200 | 200 | 403 | 403 |
| `/api/livraisons` | 200 | 200 | 403 | 403 |
| `/api/commandes` | 200 | 200 | 200 | 200 |
| `/api/photos/{id}/fichier` | 200 | 200 | 200 | 200 |

**Limite assumée.** Voir section 5.2 : la recherche reste ouverte aux quatre rôles.

### 3.3 Limiteur en mémoire non borné

`RateLimitService` bounded à 20 000 entrées, fenêtre glissante de 15 minutes, purge des compteurs expirés.

| Cible | Limite |
|---|---|
| `/api/auth/login` | 5 / IP / 15 min |
| `/api/auth/refresh` | 10 / IP / 15 min |
| `/recu/**` | 30 / IP / 15 min |

Une tentative de connexion est aussi comptée par compte. Un succès réinitialise le compteur.

**Limite assumée.** Voir section 5.4.

### 3.4 Aucun HSTS

`Strict-Transport-Security` et `Referrer-Policy` configurés dans `SecurityConfig`. En local l'API reste en HTTP, ce qui est normal : HSTS ne se déclenche que sur une requête déjà en HTTPS.

### 3.5 Durée de vie du jeton codée en dur

`AuthController` lit désormais `jwtUtil.getAccessExpirationSecondes()`.

### 3.6 Reçu public — partiellement traité

Voir section 5.1.

---

## 4. Correctifs hors audit

Ces deux bugs n'étaient pas des failles de sécurité mais des pertes de données. Ils ont été trouvés en cherchant à valider le point 3.6, et sont plus graves que la moitié de la liste ci-dessus. Ils partagent la **même cause racine**.

### 4.1 Toute création de commande échouait en 500 et perdait la commande

Deux défauts combinés :

1. `Notification.java:37` — Lombok `@Builder` ignore les initialiseurs de champ sans `@Builder.Default`. `lectures` valait donc `null`, provoquant un `NullPointerException` dans `notifierNouvelleCommande`.
2. Ce `NullPointerException` marquait la transaction de commande `rollback-only`. Le `catch` de `CommandeService` avalait l'exception **mais pas** le rollback : le commit levait `UnexpectedRollbackException` et la commande disparaissait.

**Correctif.** `@Builder.Default` sur la collection, notifications et SMS passés en `Propagation.REQUIRES_NEW`, et le `catch` muet loggue désormais l'erreur.

**Vérifié.** Commande `TK-8A93B1508F9A862D3986D34E` (27 caractères) créée, notification interne distribuée aux 3 agents, SMS envoyé.

### 4.2 La détection de rejeu de refresh token ne coupait aucune session

Même piège : l'exception levée après la révocation annulait la révocation. Le rejeu renvoyait bien `401`, mais laissait les sessions légitimes actives — c'est-à-dire que **la protection contre le vol de jeton ne protégeait rien**.

**Correctif.** `RefreshTokenReutiliseException` porte l'identifiant utilisateur, et `AuthController` déclenche la révocation hors de la transaction du contrôle.

**Vérifié.** Rejeu d'un jeton révoqué → `401`, sessions actives passées de 10 à 0, puis le jeton légitime le plus récent est aussi refusé `401`.

---

## 5. Risques restant ouverts

### 5.1 Le reçu public expose des données personnelles

`GET /recu/{numeroTicket}` est accessible sans authentification, par conception : le client reçoit ce lien par SMS et n'a pas de compte.

Mitigations en place : ticket de 96 bits (`SecureRandom`, 24 hex), `Cache-Control: no-store`, limitation à 30 vues par IP.

**Ce qui n'est pas traité :** le téléphone du client et le détail de la commande restent lisibles. Un lien fuité ou intercepté (SMS, historique de navigateur, appui long pour copier) donne accès à des données personnelles.

**Options.**
- Masquer le téléphone sur le reçu (compromis : le client ne reconnaît plus son numéro).
- Exiger une authentification (compromis : le lien SMS ne fonctionne plus).
- Les deux, avec un lien à durée de vie courte.

### 5.2 La recherche client permet d'énumérer la base

`/api/clients/recherche/nom` et `/api/clients/recherche/telephone` restent accessibles aux quatre rôles, pour ne pas casser la prise de commande par l'agent.

La réponse est paginée, ce qui rend un dump de la base clients possible. À arbitrer selon la taille de la base.

### 5.3 `X-Forwarded-For` activable sans filtre de proxies

Quand `RATE_LIMIT_PROXY_CONFIANCE=true`, l'intercepteur prend la **première** valeur de l'en-tête (`RateLimitInterceptor.java:99`). Si le reverse proxy ne supprime pas l'en-tête fourni par le client, un attaquant forge une IP arbitraire à chaque requête et le limiteur redevient contournable — le point 2.4 réapparaît.

Il faut que le proxy n'accepte que les requêtes venant de lui et réécrive l'en-tête.

### 5.4 Limiteur non partagé entre instances

Les compteurs vivent en mémoire JVM. Avec plusieurs répliques, chaque instance compte séparément et la limite effective est multipliée par le nombre de réplicas. Passer à Redis pour un déploiement multi-instance.

---

## 6. Vérifications passées

| Vérification | Résultat |
|---|---|
| `mvnw test` | 1 test, 0 échec, `BUILD SUCCESS` |
| `npm run build` | Succès (avertissement NG8113 préexistant, hors périmètre) |
| Reçu sans jeton | `200` + `Cache-Control: no-store` |
| Reçu, 33 requêtes | `429` après 30 |
| Tri invalide (`?sort=bogus`) | `400` sur `/api/tarifs`, `/api/clients`, `/api/commandes` |
| Photo sans jeton / avec jeton | `401` / `200` |
| Compte désactivé | `403` à la connexion, `401` sur session existante |
| Rotation refresh | Ancien jeton `401`, nouveau jeton `200` |
| Rejeu refresh | `401` + révocation de toutes les sessions |
| Matrice 4 rôles | Conforme au tableau 3.2 |

---

## 7. Avant mise en production

1. Poser `APP_SECURITY_MODE=production` et `JWT_SECRET`.
2. Ne pas activer `RATE_LIMIT_PROXY_CONFIANCE` avant d'avoir configuré le proxy de confiance.
3. Servir l'API en HTTPS derrière un reverse proxy.
4. Trancher le point 5.1 (reçu) et le point 5.2 (recherche client).
5. Redis pour le limiteur si déploiement multi-instance.
