import { Component, computed, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { DateFrPipe } from '../../shared/pipes/date.fr.pipe';
import { Icon } from '../../shared/icon/icon';
import { StatutBadge } from '../../shared/statut-badge/statut-badge';
import { Pagination } from '../../shared/pagination/pagination';
import { LIBELLE_STATUT_COMMANDE } from '../../shared/libelles';
import { STATUT_COMMANDE, StatutCommande } from '../../core/config/constants';
import { CommandeService } from '../../core/services/commande.service';
import { CommandeDTO, LigneCommandeLegere } from '../../core/models/commande.model';
import { TenantService } from '../../core/services/tenant.service';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

interface ColonneKanban {
  id: string;
  titre: string;
  sousTitre: string;
  statuts: StatutCommande[];
  ton: 'neutre' | 'ambre' | 'bleu' | 'vert' | 'sombre';
  suivant: StatutCommande | null;
}

@Component({
  selector: 'app-commandes',
  imports: [FormsModule, CfaPipe, DateFrPipe, Icon, StatutBadge, Pagination],
  templateUrl: './commandes.html',
  styleUrls: ['../_feature.scss', './commandes.scss'],
})
export class Commandes {
  private readonly service = inject(CommandeService);
  private readonly tenant = inject(TenantService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly peutEcrire = this.tenant.peutEcrire;
  readonly peutGererStatuts = this.tenant.peutGererStatuts;

  readonly vue = signal<'atelier' | 'liste' | 'archive'>('atelier');

  readonly commandes = signal<CommandeDTO[]>([]);
  readonly enChargement = signal(true);
  readonly totalPages = signal(0);
  readonly page = signal(0);
  readonly taille = signal(20);
  readonly erreur = signal('');
  readonly enArchivage = signal<number | null>(null);

  readonly filtreStatut = signal('');
  readonly filtreTicket = signal('');
  readonly statuts = Object.values(STATUT_COMMANDE) as StatutCommande[];

  readonly recherche = signal('');
  readonly filtrePriorite = signal<'all' | 'retard' | 'standard'>('all');

  readonly clientFiltre = signal<{ id: number; nom: string } | null>(null);

  readonly tri = signal<{ colonne: string; sens: 'asc' | 'desc' }>({ colonne: 'dateCreation', sens: 'desc' });

  readonly colonnes: ColonneKanban[] = [
    {
      id: 'recu',
      titre: '1. Reçu / En attente',
      sousTitre: 'Étiquetage & tri textile',
      statuts: [STATUT_COMMANDE.RECU, STATUT_COMMANDE.INVENTAIRE],
      ton: 'neutre',
      suivant: STATUT_COMMANDE.EN_LAVAGE,
    },
    {
      id: 'lavage',
      titre: '2. Lavage & Détachage',
      sousTitre: 'Tambour doux & solvant éco',
      statuts: [STATUT_COMMANDE.EN_LAVAGE],
      ton: 'ambre',
      suivant: STATUT_COMMANDE.REPASSAGE,
    },
    {
      id: 'repassage',
      titre: '3. Repassage & Finition',
      sousTitre: 'Vapeur & amidonnage',
      statuts: [STATUT_COMMANDE.REPASSAGE],
      ton: 'bleu',
      suivant: STATUT_COMMANDE.PRET,
    },
    {
      id: 'pret',
      titre: '4. Prêt au retrait',
      sousTitre: 'Sous housse / en casier',
      statuts: [STATUT_COMMANDE.PRET],
      ton: 'vert',
      suivant: STATUT_COMMANDE.RECUPERE,
    },
    {
      id: 'cloture',
      titre: '5. Livré & Clôturé',
      sousTitre: 'Remis au client · soldé',
      statuts: [STATUT_COMMANDE.RECUPERE, STATUT_COMMANDE.LIVRE, STATUT_COMMANDE.PAYEE],
      ton: 'sombre',
      suivant: null,
    },
  ];

  readonly piecesEnAtelier = computed(() =>
    this.commandes()
      .filter((c) => !this.estCloture(c))
      .reduce((n, c) => n + this.piecesDe(c), 0),
  );

  readonly enCours = computed(
    () =>
      this.commandes().filter(
        (c) => c.statut === STATUT_COMMANDE.EN_LAVAGE || c.statut === STATUT_COMMANDE.REPASSAGE,
      ).length,
  );

  readonly prets = computed(
    () => this.commandes().filter((c) => c.statut === STATUT_COMMANDE.PRET).length,
  );

  readonly enRetard = computed(() => this.commandes().filter((c) => this.estEnRetard(c)).length);

  readonly commandesFiltrees = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const p = this.filtrePriorite();
    return this.commandes().filter((c) => {
      const okQ =
        !q ||
        (c.numeroTicket ?? '').toLowerCase().includes(q) ||
        (c.client?.nom ?? '').toLowerCase().includes(q) ||
        (c.client?.telephone ?? '').includes(q);
      const retard = this.estEnRetard(c);
      const okP = p === 'all' || (p === 'retard' && retard) || (p === 'standard' && !retard);
      return okQ && okP;
    });
  });

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
    const atelier = this.vue() === 'atelier';
    const archive = this.vue() === 'archive';
    this.service
      .lister({
        clientId: this.clientFiltre()?.id,
        statut: atelier || archive ? undefined : this.filtreStatut() || undefined,
        archivee: archive,
        page: atelier ? 0 : this.page(),
        size: atelier ? 200 : this.taille(),
        sort: archive
          ? 'dateArchivage,desc'
          : atelier
            ? 'dateCreation,desc'
            : `${this.tri().colonne},${this.tri().sens}`,
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

  changerVue(v: 'atelier' | 'liste' | 'archive'): void {
    if (v === this.vue()) return;
    this.vue.set(v);
    this.page.set(0);
    this.charger();
  }

  /** Cartes visibles d'une colonne (recherche + filtre priorité appliqués). */
  cartesDe(col: ColonneKanban): CommandeDTO[] {
    return this.commandesFiltrees().filter((c) =>
      col.statuts.includes(c.statut as StatutCommande),
    );
  }

  prochainStatut(c: CommandeDTO): StatutCommande | null {
    const col = this.colonnes.find((x) => x.statuts.includes(c.statut as StatutCommande));
    return col?.suivant ?? null;
  }

  avancer(c: CommandeDTO): void {
    const suivant = this.prochainStatut(c);
    if (suivant) this.changerStatut(c, suivant);
  }

  estCloture(c: CommandeDTO): boolean {
    return (
      c.statut === STATUT_COMMANDE.RECUPERE ||
      c.statut === STATUT_COMMANDE.LIVRE ||
      c.statut === STATUT_COMMANDE.PAYEE
    );
  }

  estEnRetard(c: CommandeDTO): boolean {
    if (this.estCloture(c)) return false;
    if (!c.dateRecuperationPrevue) return false;
    return new Date(c.dateRecuperationPrevue).getTime() < Date.now();
  }

  piecesDe(c: CommandeDTO): number {
    return (c.lignes ?? [])
      .filter((l) => l.quantite)
      .reduce((n, l) => n + (l.quantite ?? 0), 0);
  }

  resume(c: CommandeDTO): string {
    const lignes = c.lignes ?? [];
    const pieces = lignes.filter((l) => l.quantite).reduce((n, l) => n + (l.quantite ?? 0), 0);
    const kilos = lignes.filter((l) => l.poids).reduce((n, l) => n + (l.poids ?? 0), 0);
    const parts: string[] = [];
    if (pieces > 0) parts.push(`${pieces} pc${pieces > 1 ? 's' : ''}`);
    if (kilos > 0) parts.push(`${Math.round(kilos * 10) / 10} kg`);
    return parts.join(' + ') || `${lignes.length} art.`;
  }

  apercuLigne(l: LigneCommandeLegere): string {
    if (l.poids) return `${l.poids} kg`;
    if (l.quantite) return `${l.quantite}x`;
    return '';
  }

  nomTarif(l: LigneCommandeLegere): string {
    return l.tarif?.nom || l.tarif?.typevetement || 'Article';
  }

  aPhoto(c: CommandeDTO): boolean {
    return (c.lignes ?? []).some((l) => (l.photos ?? []).length > 0);
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

  changerPage(p: number): void {
    this.page.set(p);
    this.charger();
  }

  changerTaille(t: number): void {
    this.taille.set(t);
    this.page.set(0);
    this.charger();
  }

  nouveau(): void {
    this.router.navigate(['/commandes/nouvelle']);
  }

  ouvrirDetail(c: CommandeDTO): void {
    this.router.navigate(['/commandes', c.idcommande]);
  }

  libelleStatut(s: string | undefined): string {
    return s ? (LIBELLE_STATUT_COMMANDE[s as StatutCommande] ?? s) : '';
  }

  changerStatut(c: CommandeDTO, statut: StatutCommande): void {
    if (!statut || statut === c.statut) return;
    this.erreur.set('');
    this.service.changerStatut(c.idcommande!, statut).subscribe({
      next: () => this.charger(),
      error: (err) => {
        this.erreur.set(extraireMessageErreur(err));
        this.charger();
      },
    });
  }

  estLivre(c: CommandeDTO): boolean {
    return c.statut === STATUT_COMMANDE.LIVRE;
  }

  /** Archive un ticket livré : il disparaît de l'atelier et de la liste active. */
  archiver(c: CommandeDTO): void {
    if (!c.idcommande || this.enArchivage()) return;
    this.erreur.set('');
    this.enArchivage.set(c.idcommande);
    this.service.archiver(c.idcommande).subscribe({
      next: () => {
        this.enArchivage.set(null);
        this.charger();
      },
      error: (err) => {
        this.enArchivage.set(null);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  restaurer(c: CommandeDTO): void {
    if (!c.idcommande || this.enArchivage()) return;
    this.erreur.set('');
    this.enArchivage.set(c.idcommande);
    this.service.restaurer(c.idcommande).subscribe({
      next: () => {
        this.enArchivage.set(null);
        this.charger();
      },
      error: (err) => {
        this.enArchivage.set(null);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  filtrerTicket(): void {
    const q = this.filtreTicket().trim();
    if (!q) {
      this.charger();
      return;
    }
    this.erreur.set('');
    this.enChargement.set(true);
    this.service.lister({ clientId: this.clientFiltre()?.id, page: 0, size: 50 }).subscribe({
      next: (p) => {
        const tous = (p.content ?? []) as CommandeDTO[];
        const res = tous.filter((c) => c.numeroTicket?.toUpperCase().includes(q.toUpperCase()));
        this.commandes.set(res);
        this.totalPages.set(1);
        this.enChargement.set(false);
      },
      error: (err) => {
        this.enChargement.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }
}
