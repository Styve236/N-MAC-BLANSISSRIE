import { Component, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { DateFrPipe } from '../../shared/pipes/date.fr.pipe';
import { Icon } from '../../shared/icon/icon';
import { Pagination } from '../../shared/pagination/pagination';
import { CaisseService } from '../../core/services/caisse.service';
import { ClotureDTO, DetailParMoyen } from '../../core/models/caisse.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-caisse',
  imports: [FormsModule, CfaPipe, DateFrPipe, Icon, Pagination],
  templateUrl: './caisse.html',
  styleUrls: ['../_feature.scss', './caisse.scss'],
})
export class Caisse {
  private readonly service = inject(CaisseService);

  readonly date = signal(this.aujourdhui());
  readonly rapport = signal<ClotureDTO | null>(null);
  readonly historique = signal<ClotureDTO[]>([]);
  readonly enChargement = signal(true);
  readonly erreur = signal('');
  readonly message = signal('');

  readonly filtreMoyen = signal<string>('all');

  readonly histPage = signal(0);
  readonly histTaille = signal(10);
  readonly histTotalPages = computed(() => Math.max(1, Math.ceil(this.historique().length / this.histTaille())));
  readonly historiqueVisible = computed(() => {
    const debut = this.histPage() * this.histTaille();
    return this.historique().slice(debut, debut + this.histTaille());
  });

  readonly montantEnCaisse = signal<number | null>(null);
  readonly observations = signal('');
  readonly enCoursCloture = signal(false);

  readonly lignesMoyens = computed(() => this.parMoyen(this.rapport()));

  readonly totalEncaisse = computed(() => {
    const r = this.rapport();
    if (!r) return 0;
    return r.totalLogiciel ?? this.lignesMoyens().reduce((s, m) => s + (m.montant ?? 0), 0);
  });

  readonly moyensDisponibles = computed(() => {
    const vus = new Set<string>();
    for (const m of this.lignesMoyens()) {
      if (m.moyen) vus.add(m.moyen);
    }
    return [
      { value: 'all', label: 'Tous' },
      { value: 'ESPECES', label: 'Espèces' },
      { value: 'MTN_MONEY', label: 'MTN MoMo' },
      { value: 'ORANGE_MONEY', label: 'Orange Money' },
    ].filter((o) => o.value === 'all' || vus.has(o.value));
  });

  readonly journal = computed(() => {
    const f = this.filtreMoyen();
    return this.lignesMoyens().filter((m) => f === 'all' || m.moyen === f);
  });

  readonly partLignes = computed(() =>
    this.lignesMoyens().map((m) => ({
      ...m,
      part: this.totalEncaisse() ? Math.round(((m.montant ?? 0) / this.totalEncaisse()) * 100) : 0,
    })),
  );

  constructor() {
    this.chargerRapport();
    this.chargerHistorique();
  }

  private aujourdhui(): string {
    return new Date().toISOString().slice(0, 10);
  }

  chargerRapport(): void {
    this.enChargement.set(true);
    this.erreur.set('');
    this.service.rapport(this.date()).subscribe({
      next: (r) => {
        this.rapport.set(r);
        this.enChargement.set(false);
      },
      error: (err) => {
        this.enChargement.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  chargerHistorique(): void {
    this.histPage.set(0);
    this.service.historique().subscribe({
      next: (h) => this.historique.set(h ?? []),
      error: () => undefined,
    });
  }

  parMoyen(r: ClotureDTO | null): DetailParMoyen[] {
    if (!r) return [];
    if (r.parMoyen && r.parMoyen.length > 0) return r.parMoyen;
    if (r.detailParMoyen) {
      return r.detailParMoyen
        .split(';')
        .filter(Boolean)
        .map((part) => {
          const [moyen, montant, nombre] = part.split(':');
          return { moyen, montant: Number(montant), nombre: Number(nombre) };
        });
    }
    return [];
  }

  libelleMoyen(m: string): string {
    const map: Record<string, string> = {
      ESPECES: 'Espèces',
      ORANGE_MONEY: 'Orange Money',
      MTN_MONEY: 'MTN MoMo',
    };
    return map[m] ?? m;
  }

  partDe(moyen: string): number {
    const total = this.totalEncaisse();
    if (!total) return 0;
    const ligne = this.lignesMoyens().find((m) => m.moyen === moyen);
    return Math.round(((ligne?.montant ?? 0) / total) * 100);
  }

  montantDe(moyen: string): number {
    return this.lignesMoyens().find((m) => m.moyen === moyen)?.montant ?? 0;
  }

  cloturer(): void {
    const montant = this.montantEnCaisse();
    if (montant === null || montant < 0) {
      this.erreur.set('Saisissez le montant compté en caisse.');
      return;
    }
    this.enCoursCloture.set(true);
    this.erreur.set('');
    this.service
      .cloturer({ date: this.date(), montantEnCaisse: montant, observations: this.observations() || undefined })
      .subscribe({
        next: (r) => {
          this.enCoursCloture.set(false);
          this.rapport.set(r);
          this.message.set('Caisse clôturée avec succès.');
          this.chargerHistorique();
        },
        error: (err) => {
          this.enCoursCloture.set(false);
          this.erreur.set(extraireMessageErreur(err));
        },
      });
  }

  ticket(): void {
    this.service.ticket(this.date()).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 30000);
      },
      error: (err) => this.erreur.set(extraireMessageErreur(err)),
    });
  }

  changerHistPage(p: number): void {
    this.histPage.set(p);
  }

  changerHistTaille(t: number): void {
    this.histTaille.set(t);
    this.histPage.set(0);
  }
}
