import { HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  let request = req;
  if (token) {
    request = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(request).pipe(
    catchError((err) => {
      const peutRafraichir =
        err?.status === 401 &&
        !req.url.includes('/api/auth/login') &&
        !req.url.includes('/api/auth/refresh') &&
        auth.getToken() != null;

      if (!peutRafraichir) {
        return throwError(() => err);
      }

      return auth.refresh().pipe(
        switchMap((session) => {
          const retry = req.clone({
            setHeaders: { Authorization: `Bearer ${session.token}` },
          });
          return next(retry);
        }),
        catchError((refreshErr) => {
          auth.logout().subscribe();
          return throwError(() => refreshErr);
        })
      );
    })
  );
};