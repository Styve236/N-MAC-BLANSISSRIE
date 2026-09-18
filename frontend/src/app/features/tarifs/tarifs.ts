import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { Icon } from '../../shared/icon/icon';
import { typeNettoyageOptions } from '../../shared/libelles';
import { TarifService } from '../../core/services/tarif.service';
import { TenantService } from '../../core/services/tenant.service';
import { Tarif } from '../../core/models/commande.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-tarifs',
  imports: [FormsModule, CfaPipe, Icon],
  templateUrl: './tarifs.html',
  styleUrl: '../_feature.scss',
})
export class Tarifs {
  private readonly service = inject(TarifService);
  private readonly tenant = inject(TenantService);

  readonly estAdmin = this.tenant.estAdmin;
  readonly typesNettoyage = typeNettoyageOptions();

  readonly tarifs = signal<Tarif[]>([]);
  readonly enChargement = signal(true);
  readonly totalPages = signal(0);
  readonly page = signal(0);
  readonly filtreType = signal('');
  readonly erreur = signal('');
  readonly message = signal('');

  readonly showForm = signal(false);
  readonly form = signal<Tarif>({});
  readonly enregistrement = signal(false);

  constructor() {
    this.charger();
  }

  charger(): void {
    this.enChargement.set(true);
    this.service.lister(this.filtreType() || undefined, this.page(), 20).subscribe({
      next: (p) => {
        this.tarifs.set(p.content ?? []);
        this.totalPages.set(p.totalPages ?? 0);
        this.enChargement.set(false);
      },
      error: () => this.enChargement.set(false),
    });
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

  ouvrirNouveau(): void {
    this.form.set({ actif: true });
    this.showForm.set(true);
    this.erreur.set('');
  }

  ouvrirEdition(t: Tarif): void {
    this.form.set({ ...t });
    this.showForm.set(true);
    this.erreur.set('');
  }

  enregistrer(): void {
    const f = this.form();
    if (!f.nom) {
      this.erreur.set('Le nom est obligatoire.');
      return;
    }
    this.enregistrement.set(true);
    const appel = f.idtarif
      ? this.service.mettreAJour(f.idtarif, f)
      : this.service.creer(f);

    appel.subscribe({
      next: () => {
        this.enregistrement.set(false);
        this.showForm.set(false);
        this.message.set('Tarif enregistré.');
        this.charger();
      },
      error: (err) => {
        this.enregistrement.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  basculerActif(t: Tarif): void {
    const id = t.idtarif!;
    const appel: Observable<void> = t.actif
      ? this.service.supprimer(id)
      : this.service.activer(id).pipe(map(() => undefined));
    appel.subscribe({
      next: () => this.charger(),
      error: (err) => this.erreur.set(extraireMessageErreur(err)),
    });
  }

  annuler(): void {
    this.showForm.set(false);
    this.erreur.set('');
  }
}