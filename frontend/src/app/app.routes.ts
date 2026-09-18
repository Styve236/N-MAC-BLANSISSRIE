import { Routes } from '@angular/router';
import { MainLayout } from './layouts/main-layout/main-layout';
import { AuthLayout } from './layouts/auth-layout/auth-layout';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { ROLES } from './core/config/constants';

export const routes: Routes = [
  {
    path: 'login',
    component: AuthLayout,
    children: [{ path: '', loadComponent: () => import('./features/login/login').then((m) => m.Login) }],
  },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        canActivate: [roleGuard(ROLES.ADMIN, ROLES.RECEPTIONNISTE)],
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'clients',
        canActivate: [roleGuard(ROLES.ADMIN, ROLES.RECEPTIONNISTE)],
        loadComponent: () => import('./features/clients/clients').then((m) => m.Clients),
      },
      {
        path: 'commandes',
        canActivate: [roleGuard(ROLES.ADMIN, ROLES.RECEPTIONNISTE, ROLES.AGENT_PRODUCTION)],
        children: [
          {
            path: '',
            loadComponent: () => import('./features/commandes/commandes').then((m) => m.Commandes),
          },
          {
            path: 'nouvelle',
            canActivate: [roleGuard(ROLES.ADMIN, ROLES.RECEPTIONNISTE)],
            loadComponent: () =>
              import('./features/commandes/nouvelle-commande').then((m) => m.NouvelleCommande),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/commandes/detail-commande').then((m) => m.DetailCommande),
          },
        ],
      },
      {
        path: 'livraisons',
        canActivate: [roleGuard(ROLES.ADMIN, ROLES.RECEPTIONNISTE, ROLES.LIVREUR)],
        loadComponent: () => import('./features/livraisons/livraisons').then((m) => m.Livraisons),
      },
      {
        path: 'tarifs',
        canActivate: [roleGuard(ROLES.ADMIN, ROLES.RECEPTIONNISTE)],
        loadComponent: () => import('./features/tarifs/tarifs').then((m) => m.Tarifs),
      },
      {
        path: 'inventaire',
        canActivate: [roleGuard(ROLES.ADMIN, ROLES.RECEPTIONNISTE)],
        loadComponent: () => import('./features/inventaire/inventaire').then((m) => m.Inventaire),
      },
      {
        path: 'fidelite',
        canActivate: [roleGuard(ROLES.ADMIN, ROLES.RECEPTIONNISTE)],
        loadComponent: () => import('./features/fidelite/fidelite').then((m) => m.Fidelite),
      },
      {
        path: 'caisse',
        canActivate: [roleGuard(ROLES.ADMIN, ROLES.RECEPTIONNISTE)],
        loadComponent: () => import('./features/caisse/caisse').then((m) => m.Caisse),
      },
      {
        path: 'rapports',
        canActivate: [roleGuard(ROLES.ADMIN, ROLES.RECEPTIONNISTE)],
        loadComponent: () => import('./features/rapports/rapports').then((m) => m.Rapports),
      },
      {
        path: 'users',
        canActivate: [roleGuard(ROLES.ADMIN)],
        loadComponent: () => import('./features/users/users').then((m) => m.Users),
      },
    ],
  },
  { path: '**', redirectTo: '/dashboard' },
];