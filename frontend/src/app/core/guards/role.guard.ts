import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ROLES } from '../config/constants';

export function roleGuard(...rolesAutorises: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isAuthenticated()) {
      return router.createUrlTree(['/login']);
    }
    if (rolesAutorises.includes(auth.role() ?? '')) {
      return true;
    }
    if (auth.role() === ROLES.AGENT_PRODUCTION) {
      return router.createUrlTree(['/commandes']);
    }
    if (auth.role() === ROLES.LIVREUR) {
      return router.createUrlTree(['/livraisons']);
    }
    return router.createUrlTree(['/dashboard']);
  };
}