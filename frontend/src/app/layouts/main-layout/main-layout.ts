import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { fromEvent, interval } from 'rxjs';
import { Icon } from '../../shared/icon/icon';
import { ToastHost } from '../../shared/toast-host/toast-host';
import { AuthService } from '../../core/services/auth.service';
import { TenantService } from '../../core/services/tenant.service';
import { NotificationService } from '../../core/services/notification.service';
import { ToastService } from '../../core/services/toast.service';
import { NotificationDTO } from '../../core/models/notification.model';
import { ROLES } from '../../core/config/constants';

interface MenuItem {
  label: string;
  route: string;
  icon: string;
  roles: string[];
}

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon, ToastHost],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout {
  private readonly tenant = inject(TenantService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifsSvc = inject(NotificationService);
  private readonly toasts = inject(ToastService);

  readonly nom = this.tenant.nom;
  readonly role = this.tenant.role;

  readonly notifs = signal<NotificationDTO[]>([]);
  readonly nonLues = signal(0);
  readonly ouvertes = signal(false);
  private readonly precedentCompte = signal(-1);

  constructor() {
    this.rafraichirNotifs();
    interval(5000)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.rafraichirNotifs());
    fromEvent(document, 'visibilitychange')
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        if (!document.hidden) this.rafraichirNotifs();
      });
  }

  readonly menus: MenuItem[] = [
    { label: 'Tableau de bord', route: '/dashboard', icon: 'dashboard', roles: [ROLES.ADMIN, ROLES.RECEPTIONNISTE] },
    { label: 'Clients', route: '/clients', icon: 'users', roles: [ROLES.ADMIN, ROLES.RECEPTIONNISTE] },
    { label: 'Commandes', route: '/commandes', icon: 'commandes', roles: [ROLES.ADMIN, ROLES.RECEPTIONNISTE, ROLES.AGENT_PRODUCTION] },
    { label: 'Livraisons', route: '/livraisons', icon: 'livraisons', roles: [ROLES.ADMIN, ROLES.RECEPTIONNISTE, ROLES.LIVREUR] },
    { label: 'Tarifs', route: '/tarifs', icon: 'tarifs', roles: [ROLES.ADMIN, ROLES.RECEPTIONNISTE] },
    { label: 'Inventaire', route: '/inventaire', icon: 'inventaire', roles: [ROLES.ADMIN, ROLES.RECEPTIONNISTE] },
    { label: 'Fidélité', route: '/fidelite', icon: 'fidelite', roles: [ROLES.ADMIN, ROLES.RECEPTIONNISTE] },
    { label: 'Caisse', route: '/caisse', icon: 'caisse', roles: [ROLES.ADMIN, ROLES.RECEPTIONNISTE] },
    { label: 'Rapports', route: '/rapports', icon: 'rapports', roles: [ROLES.ADMIN, ROLES.RECEPTIONNISTE] },
    { label: 'Utilisateurs', route: '/users', icon: 'personnel', roles: [ROLES.ADMIN] },
  ];

  readonly menusVisibles = computed(() => this.menus.filter((m) => m.roles.includes(this.role() ?? '')));

  rafraichirNotifs(): void {
    this.notifsSvc.mesNotifications().subscribe({
      next: (liste) => {
        const nouveau = liste.filter((n) => !n.lue).length;
        const ancien = this.precedentCompte();
        this.notifs.set(liste);
        this.nonLues.set(nouveau);
        if (ancien >= 0 && nouveau > ancien) {
          const nonLues = liste.filter((n) => !n.lue);
          this.toasts.info(nonLues[0]?.message ?? 'Nouvelle notification');
          this.bip();
        }
        this.precedentCompte.set(nouveau);
      },
      error: () => undefined,
    });
  }

  basculerNotifs(): void {
    this.ouvertes.update((o) => !o);
    if (this.ouvertes()) {
      this.rafraichirNotifs();
    }
  }

  toutLue(): void {
    this.notifsSvc.toutLue().subscribe({
      next: () => {
        this.notifs.update((liste) => liste.map((n) => ({ ...n, lue: true })));
        this.nonLues.set(0);
      },
      error: () => undefined,
    });
  }

  ouvrirNotif(n: NotificationDTO): void {
    if (!n.lue) {
      this.notifsSvc.marquerLue(n.id).subscribe({
        next: () => {
          this.notifs.update((liste) => liste.map((x) => (x.id === n.id ? { ...x, lue: true } : x)));
          this.nonLues.update((c) => Math.max(0, c - 1));
        },
        error: () => undefined,
      });
    }
    this.ouvertes.set(false);
    if (n.commandeId) {
      this.router.navigate(['/commandes', n.commandeId]);
    }
  }

  heureRelative(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    const diff = Date.now() - d.getTime();
    const min = Math.floor(diff / 60000);
    if (min < 1) return "à l'instant";
    if (min < 60) return `il y a ${min} min`;
    const h = Math.floor(min / 60);
    if (h < 24) return `il y a ${h} h`;
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
  }

  deconnexion(): void {
    this.auth.logout().subscribe({
      complete: () => this.router.navigate(['/login']),
    });
  }

  private bip(): void {
    try {
      const AudioCtx = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.frequency.value = 880;
      gain.gain.value = 0.12;
      osc.start();
      osc.stop(ctx.currentTime + 0.25);
    } catch {
      // aucun beep disponible (bloqué par le navigateur) : seule la notification visuelle reste
    }
  }
}