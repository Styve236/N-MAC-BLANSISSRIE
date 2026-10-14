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
  const body = err.error as { message?: string; erreurs?: string[]; status?: number };
  if (err.status === 401) {
    return body?.message ?? 'Authentification requise.';
  }
  if (err.status === 403) {
    return body?.message ?? 'Accès refusé.';
  }
  if (err.status === 429) {
    return body?.message ?? 'Trop de tentatives, réessayez plus tard.';
  }
  if (err.status >= 500) {
    return body?.message ?? 'Le serveur a rencontré un problème. Réessayez dans un instant.';
  }
  if (Array.isArray(body?.erreurs) && body!.erreurs!.length > 0) {
    return body!.erreurs!.join(' • ');
  }
  if (body?.message) {
    return body.message;
  }
  return `Erreur ${err.status} lors de la requête.`;
}