import { Component, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../../shared/icon/icon';
import { Pagination } from '../../shared/pagination/pagination';
import { DateFrPipe } from '../../shared/pipes/date.fr.pipe';
import { InventaireService } from '../../core/services/inventaire.service';
import { TenantService } from '../../core/services/tenant.service';
import { VetementDTO, BonInventaireDTO } from '../../core/models/inventaire.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

interface LigneBonForm {
  vetementId: number | null;
  sens: 'entree' | 'sortie';
  quantite: number | null;
  etat: string;
}

@Component({
  selector: 'app-inventaire',
  imports: [FormsModule, Icon, DateFrPipe, Pagination],
  templateUrl: './inventaire.html',
  styleUrl: '../_feature.scss',
})
export class Inventaire {
  private readonly service = inject(InventaireService);
  private readonly tenant = inject(TenantService);

  readonly estAdmin = this.tenant.estAdmin;

  readonly onglet = signal<'vetements' | 'bons'>('vetements');
  readonly vetements = signal<VetementDTO[]>([]);
  readonly bons = signal<BonInventaireDTO[]>([]);
  readonly enChargement = signal(true);
  readonly erreur = signal('');
  readonly message = signal('');

  readonly vetementPage = signal(0);
  readonly vetementTaille = signal(20);
  readonly vetementTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.vetements().length / this.vetementTaille())),
  );
  readonly vetementsVisible = computed(() => {
    const debut = this.vetementPage() * this.vetementTaille();
    return this.vetements().slice(debut, debut + this.vetementTaille());
  });

  readonly bonPage = signal(0);
  readonly bonTaille = signal(10);
  readonly bonTotalPages = computed(() => Math.max(1, Math.ceil(this.bons().length / this.bonTaille())));
  readonly bonsVisible = computed(() => {
    const debut = this.bonPage() * this.bonTaille();
    return this.bons().slice(debut, debut + this.bonTaille());
  });

  readonly showVetementForm = signal(false);
  readonly vetementForm = signal<VetementDTO>({});
  readonly enregistrementVetement = signal(false);

  readonly showBonForm = signal(false);
  readonly bonForm = signal({ date: new Date().toISOString().slice(0, 10), observations: '' });
  readonly lignesBon = signal<LigneBonForm[]>([this.ligneVide()]);
  readonly enregistrementBon = signal(false);

  constructor() {
    this.chargerVetements();
    this.chargerBons();
  }

  switcher(onglet: 'vetements' | 'bons'): void {
    this.onglet.set(onglet);
  }

  chargerVetements(): void {
    this.enChargement.set(true);
    this.vetementPage.set(0);
    this.service.vetements().subscribe({
      next: (v) => {
        this.vetements.set(v ?? []);
        this.enChargement.set(false);
      },
      error: () => this.enChargement.set(false),
    });
  }

  chargerBons(): void {
    this.bonPage.set(0);
    this.service.bons().subscribe({
      next: (b) => this.bons.set(b ?? []),
      error: () => undefined,
    });
  }

  nettoyerMessages(): void {
    this.erreur.set('');
    this.message.set('');
  }

  changerVetementPage(p: number): void {
    this.vetementPage.set(p);
  }

  changerVetementTaille(t: number): void {
    this.vetementTaille.set(t);
    this.vetementPage.set(0);
  }

  changerBonPage(p: number): void {
    this.bonPage.set(p);
  }

  changerBonTaille(t: number): void {
    this.bonTaille.set(t);
    this.bonPage.set(0);
  }

  ouvrirNouveauVetement(): void {
    this.nettoyerMessages();
    this.vetementForm.set({ libelle: '', typedevetement: '', quantiteStock: 0 });
    this.showVetementForm.set(true);
  }

  ouvrirEditionVetement(v: VetementDTO): void {
    this.nettoyerMessages();
    this.vetementForm.set({ ...v });
    this.showVetementForm.set(true);
  }

  enregistrerVetement(): void {
    const f = this.vetementForm();
    if (!f.libelle) {
      this.erreur.set('Le libellé est obligatoire.');
      return;
    }
    this.enregistrementVetement.set(true);
    const appel = f.idvetement ? this.service.mettreAJourVetement(f.idvetement, f) : this.service.creerVetement(f);
    appel.subscribe({
      next: () => {
        this.enregistrementVetement.set(false);
        this.showVetementForm.set(false);
        this.message.set('Article enregistré.');
        this.chargerVetements();
      },
      error: (err) => {
        this.enregistrementVetement.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  supprimerVetement(v: VetementDTO): void {
    if (!v.idvetement) return;
    if (!confirm(`Supprimer l'article « ${v.libelle} » ?`)) return;
    this.service.supprimerVetement(v.idvetement).subscribe({
      next: () => {
        this.message.set('Article supprimé.');
        this.chargerVetements();
      },
      error: (err) => this.erreur.set(extraireMessageErreur(err)),
    });
  }

  ligneVide(): LigneBonForm {
    return { vetementId: null, sens: 'entree', quantite: null, etat: 'BON' };
  }

  ajouterLigneBon(): void {
    this.lignesBon.update((l) => [...l, this.ligneVide()]);
  }

  retirerLigneBon(i: number): void {
    this.lignesBon.update((l) => l.filter((_, idx) => idx !== i));
  }

  enregistrerBon(): void {
    const lignes = this.lignesBon()
      .filter((l) => l.vetementId && l.quantite)
      .map((l) => ({
        vetementId: l.vetementId!,
        quantite: l.sens === 'entree' ? Math.abs(l.quantite!) : -Math.abs(l.quantite!),
        etat: l.etat || 'BON',
      }));

    if (lignes.length === 0) {
      this.erreur.set('Ajoutez au moins une ligne au bon.');
      return;
    }

    this.enregistrementBon.set(true);
    this.erreur.set('');
    this.service.creerBon({ date: this.bonForm().date, observations: this.bonForm().observations || undefined, lignes }).subscribe({
      next: () => {
        this.enregistrementBon.set(false);
        this.showBonForm.set(false);
        this.lignesBon.set([this.ligneVide()]);
        this.message.set('Bon d\u2019inventaire créé.');
        this.chargerBons();
        this.chargerVetements();
      },
      error: (err) => {
        this.enregistrementBon.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }
}