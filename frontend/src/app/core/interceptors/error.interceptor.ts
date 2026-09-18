import { HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  return next(req);
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
  if (Array.isArray(body?.erreurs) && body!.erreurs!.length > 0) {
    return body!.erreurs!.join(' • ');
  }
  if (body?.message) {
    return body.message;
  }
  return `Erreur ${err.status} lors de la requête.`;
}

// Ré-exporté pour homogénéité de nommage (intercepteur vide, le 401 est géré par authInterceptor).
export const apiErrorInterceptor: HttpInterceptorFn = errorInterceptor;