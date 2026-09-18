import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { DateFrPipe } from '../../shared/pipes/date.fr.pipe';
import { Icon } from '../../shared/icon/icon';
import { CaisseService } from '../../core/services/caisse.service';
import { ClotureDTO, DetailParMoyen } from '../../core/models/caisse.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-caisse',
  imports: [FormsModule, CfaPipe, DateFrPipe, Icon],
  templateUrl: './caisse.html',
  styleUrl: '../_feature.scss',
})
export class Caisse {
  private readonly service = inject(CaisseService);

  readonly date = signal(this.aujourdhui());
  readonly rapport = signal<ClotureDTO | null>(null);
  readonly historique = signal<ClotureDTO[]>([]);
  readonly enChargement = signal(true);
  readonly erreur = signal('');
  readonly message = signal('');

  readonly montantEnCaisse = signal<number | null>(null);
  readonly observations = signal('');
  readonly enCoursCloture = signal(false);

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
      MTN_MONEY: 'MTN Money',
    };
    return map[m] ?? m;
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
}