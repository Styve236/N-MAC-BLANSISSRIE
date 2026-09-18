import { Component, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { DateFrPipe } from '../../shared/pipes/date.fr.pipe';
import { Icon } from '../../shared/icon/icon';
import { StatutBadge } from '../../shared/statut-badge/statut-badge';
import { LIBELLE_STATUT_COMMANDE } from '../../shared/libelles';
import { STATUT_COMMANDE, StatutCommande } from '../../core/config/constants';
import { CommandeService } from '../../core/services/commande.service';
import { TenantService } from '../../core/services/tenant.service';
import { CommandeDTO } from '../../core/models/commande.model';

@Component({
  selector: 'app-commandes',
  imports: [FormsModule, CfaPipe, DateFrPipe, Icon, StatutBadge],
  templateUrl: './commandes.html',
  styleUrl: '../_feature.scss',
})
export class Commandes {
  private readonly service = inject(CommandeService);
  private readonly tenant = inject(TenantService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly peutEcrire = this.tenant.peutEcrire;

  readonly commandes = signal<CommandeDTO[]>([]);
  readonly enChargement = signal(true);
  readonly totalPages = signal(0);
  readonly page = signal(0);

  readonly filtreStatut = signal('');
  readonly filtreTicket = signal('');
  readonly statuts = Object.values(STATUT_COMMANDE) as StatutCommande[];

  readonly clientFiltre = signal<{ id: number; nom: string } | null>(null);

  readonly tri = signal<{ colonne: string; sens: 'asc' | 'desc' }>({ colonne: 'dateCreation', sens: 'desc' });

  constructor() {
    this.route.queryParams.subscribe((qp) => {
      const id = Number(qp['client']);
      if (id) {
        this.clientFiltre.set({ id, nom: qp['nom'] || '' });
      } else {
        this.clientFiltre.set(null);
      }
      this.page.set(0);
      this.charger();
    });
  }

  charger(): void {
    this.enChargement.set(true);
    this.service
      .lister({
        clientId: this.clientFiltre()?.id,
        statut: this.filtreStatut() || undefined,
        page: this.page(),
        size: 20,
        sort: `${this.tri().colonne},${this.tri().sens}`,
      })
      .subscribe({
        next: (p) => {
          this.commandes.set(p.content ?? []);
          this.totalPages.set(p.totalPages ?? 0);
          this.enChargement.set(false);
        },
        error: () => this.enChargement.set(false),
      });
  }

  trier(colonne: string): void {
    const t = this.tri();
    this.tri.set({
      colonne,
      sens: t.colonne === colonne && t.sens === 'asc' ? 'desc' : 'asc',
    });
    this.page.set(0);
    this.charger();
  }

  flecheTri(colonne: string): string {
    const t = this.tri();
    if (t.colonne !== colonne) return '';
    return t.sens === 'asc' ? '▲' : '▼';
  }

  retirerClient(): void {
    this.router.navigate(['/commandes'], { queryParams: { client: null, nom: null } });
  }

  filtrer(): void {
    this.page.set(0);
    this.charger();
  }

  allerPage(delta: number): void {
    const p = this.page() + delta;
    if (p >= 0 && p < this.totalPages()) {
      this.page.set(p);
      this.charger();
    }
  }

  nouveau(): void {
    this.router.navigate(['/commandes/nouvelle']);
  }

  ouvrirDetail(c: CommandeDTO): void {
    this.router.navigate(['/commandes', c.idcommande]);
  }

  libelleStatut(s: string): string {
    return LIBELLE_STATUT_COMMANDE[s as StatutCommande] ?? s;
  }

  filtrerTicket(): void {
    const q = this.filtreTicket().trim();
    if (!q) {
      this.charger();
      return;
    }
    this.service.lister({ clientId: this.clientFiltre()?.id, page: 0, size: 50 }).subscribe((p) => {
      const tous = (p.content ?? []) as CommandeDTO[];
      const res = tous.filter((c) => c.numeroTicket?.toUpperCase().includes(q.toUpperCase()));
      this.commandes.set(res);
      this.totalPages.set(1);
      this.enChargement.set(false);
    });
  }
}