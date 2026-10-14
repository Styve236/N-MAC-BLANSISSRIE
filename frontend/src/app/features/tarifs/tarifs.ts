import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { Icon } from '../../shared/icon/icon';
import { typeNettoyageOptions, LIBELLE_TYPE_NETTOYAGE } from '../../shared/libelles';
import { TarifService } from '../../core/services/tarif.service';
import { TenantService } from '../../core/services/tenant.service';
import { Tarif } from '../../core/models/commande.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-tarifs',
  imports: [FormsModule, CfaPipe, Icon],
  templateUrl: './tarifs.html',
  styleUrls: ['../_feature.scss', './tarifs.scss'],
})
export class Tarifs {
  private readonly service = inject(TarifService);
  private readonly tenant = inject(TenantService);

  readonly estAdmin = this.tenant.estAdmin;
  readonly typesNettoyage = typeNettoyageOptions();

  readonly tarifs = signal<Tarif[]>([]);
  readonly enChargement = signal(true);
  readonly recherche = signal('');
  readonly categorie = signal<string>('all');
  readonly erreur = signal('');
  readonly message = signal('');

  readonly showForm = signal(false);
  readonly form = signal<Tarif>({});
  readonly enregistrement = signal(false);

  readonly confirmation = signal<number | null>(null);
  private arretConfirmation: number | undefined;

  readonly categories = computed(() => {
    const vues = new Set<string>();
    for (const t of this.tarifs()) {
      if (t.typeNettoyage) vues.add(t.typeNettoyage);
    }
    const options = this.typesNettoyage.filter((o) => vues.has(o.value));
    return [{ value: 'all', label: 'Toutes les prestations' }, ...options];
  });

  readonly tarifsFiltres = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const cat = this.categorie();
    return this.tarifs().filter((t) => {
      const okCat = cat === 'all' || t.typeNettoyage === cat;
      const texte = `${t.nom ?? ''} ${t.typevetement ?? ''}`.toLowerCase();
      const okQ = !q || texte.includes(q);
      return okCat && okQ;
    });
  });

  readonly nbActifs = computed(() => this.tarifs().filter((t) => t.actif).length);

  constructor() {
    this.charger();
  }

  charger(): void {
    this.enChargement.set(true);
    this.service.lister(undefined, 0, 200).subscribe({
      next: (p) => {
        this.tarifs.set(p.content ?? []);
        this.enChargement.set(false);
      },
      error: () => this.enChargement.set(false),
    });
  }

  /** Un tarif facturé au poids : prix au kilo renseigné sans prix unitaire. */
  estAuPoids(t: Tarif): boolean {
    return (t.prixauklo ?? 0) > 0 && !(t.prixunitaire ?? 0);
  }

  prixTarif(t: Tarif): number | undefined {
    return this.estAuPoids(t) ? t.prixauklo : (t.prixunitaire ?? t.prixauklo);
  }

  libelleTypeNettoyage(v: string | undefined): string {
    return v ? (LIBELLE_TYPE_NETTOYAGE as Record<string, string>)[v] ?? v : '';
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

    if (t.actif && this.confirmation() !== id) {
      this.armConfirmation(id);
      return;
    }
    this.confirmation.set(null);

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

  private armConfirmation(id: number): void {
    if (this.arretConfirmation) {
      window.clearTimeout(this.arretConfirmation);
    }
    this.confirmation.set(id);
    this.arretConfirmation = window.setTimeout(() => this.confirmation.set(null), 3500);
  }
}
