import { HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

/**
 * Signalement global des pannes (reseau / serveur). Les erreurs metier 4xx
 * restent affichees au cas par cas par chaque ecran, pour eviter les doublons.
 */
export const errorInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const toasts = inject(ToastService);
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      const estRouteAuth = req.url.includes('/api/auth/');
      if (!estRouteAuth && (err.status === 0 || err.status >= 500)) {
        toasts.erreur(extraireMessageErreur(err));
      }
      return throwError(() => err);
    })
  );
};

export function extraireMessageErreur(err: HttpErrorResponse): string {
  if (err.status === 0) {
    return 'Impossible de joindre le serveur.';
  }
  // Le corps d'erreur peut être un objet JSON ({ message, erreurs }) ou une
  // simple chaîne (ex. les @ExceptionHandler du backend renvoient du texte brut).
  const body = err.error as { message?: string; erreurs?: string[]; status?: number } | string | null | undefined;
  const message = typeof body === 'string' ? body : body?.message;
  const erreurs = body !== null && typeof body === 'object' ? body.erreurs : undefined;

  if (err.status === 401) {
    return message ?? 'Authentification requise.';
  }
  if (err.status === 403) {
    return message ?? 'Accès refusé.';
  }
  if (err.status === 429) {
    return message ?? 'Trop de tentatives, réessayez plus tard.';
  }
  if (err.status >= 500) {
    return message ?? 'Le serveur a rencontré un problème. Réessayez dans un instant.';
  }
  if (Array.isArray(erreurs) && erreurs.length > 0) {
    return erreurs.join(' • ');
  }
  if (message) {
    return message;
  }
  return `Erreur ${err.status} lors de la requête.`;
}