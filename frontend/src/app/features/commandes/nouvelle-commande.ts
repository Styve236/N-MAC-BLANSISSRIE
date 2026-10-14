import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, Observable, catchError, map, of } from 'rxjs';
import { Icon } from '../../shared/icon/icon';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { moyenPaiementOptions, typeNettoyageOptions, LIBELLE_TYPE_NETTOYAGE } from '../../shared/libelles';
import { ClientService } from '../../core/services/client.service';
import { TarifService } from '../../core/services/tarif.service';
import { CommandeService } from '../../core/services/commande.service';
import { PhotoService } from '../../core/services/photo.service';
import { ToastService } from '../../core/services/toast.service';
import { TypeNettoyage as TypeNettoyageValue } from '../../core/config/constants';
import { Client } from '../../core/models/client.model';
import { Commande, CommandeRequestDTO, LigneCommandeDTO, Tarif } from '../../core/models/commande.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

interface PhotoSelection {
  fichier: File;
  apercu: string;
}

interface LigneFormulaire {
  tarifId: number | null;
  auPoids: boolean;
  poids: number | null;
  quantite: number | null;
  typeNettoyage: string;
  description: string;
  photos: PhotoSelection[];
}

@Component({
  selector: 'app-nouvelle-commande',
  imports: [FormsModule, Icon, CfaPipe],
  templateUrl: './nouvelle-commande.html',
  styleUrls: ['../_feature.scss', './nouvelle-commande.scss'],
})
export class NouvelleCommande {
  private readonly clients = inject(ClientService);
  private readonly tarifsSvc = inject(TarifService);
  private readonly commandes = inject(CommandeService);
  private readonly photos = inject(PhotoService);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);

  readonly typesNettoyage = typeNettoyageOptions();
  readonly moyensPaiement = moyenPaiementOptions();

  readonly tarifs = signal<Tarif[]>([]);
  readonly enChargementTarifs = signal(true);
  readonly rechercheTarif = signal('');
  readonly categorie = signal<string>('all');

  readonly recherchePhone = signal('');
  readonly resultats = signal<Client[]>([]);
  readonly clientSelectionne = signal<Client | null>(null);

  readonly lignes = signal<LigneFormulaire[]>([]);
  readonly dateRetrait = signal('');
  readonly acompte = signal<number | null>(null);
  readonly moyenAcompte = signal('ESPECES');
  readonly messageAgent = signal('');

  readonly enregistrement = signal(false);
  readonly erreur = signal('');

  readonly montantTotal = computed(() =>
    this.lignes().reduce((somme, l) => somme + this.montantLigne(l), 0),
  );

  readonly resteAPayer = computed(() => Math.max(0, this.montantTotal() - (this.acompte() ?? 0)));

  readonly nbPieces = computed(() =>
    this.lignes().reduce((n, l) => n + (l.auPoids ? 0 : (l.quantite ?? 0)), 0),
  );

  readonly nbKilos = computed(() =>
    this.lignes().reduce((n, l) => n + (l.auPoids ? (l.poids ?? 0) : 0), 0),
  );

  /** Catégories proposées à partir des tarifs actifs (type de nettoyage). */
  readonly categories = computed(() => {
    const vues = new Set<string>();
    for (const t of this.tarifs()) {
      if (t.typeNettoyage) vues.add(t.typeNettoyage);
    }
    const options = this.typesNettoyage.filter((o) => vues.has(o.value));
    return [{ value: 'all', label: 'Tout' }, ...options];
  });

  readonly tarifsFiltres = computed(() => {
    const q = this.rechercheTarif().trim().toLowerCase();
    const cat = this.categorie();
    return this.tarifs().filter((t) => {
      const okCat = cat === 'all' || t.typeNettoyage === cat;
      const texte = `${t.nom ?? ''} ${t.typevetement ?? ''}`.toLowerCase();
      const okQ = !q || texte.includes(q);
      return okCat && okQ;
    });
  });

  constructor() {
    this.tarifsSvc.actifs().subscribe({
      next: (t) => {
        this.tarifs.set(t);
        this.enChargementTarifs.set(false);
      },
      error: () => this.enChargementTarifs.set(false),
    });
  }

  /** Un tarif facturé au poids par défaut (prix au kilo renseigné). */
  estAuPoids(t: Tarif): boolean {
    const auKilo = (t.prixauklo ?? 0) > 0;
    const aLaPiece = (t.prixunitaire ?? 0) > 0;
    return auKilo && !aLaPiece;
  }

  libelleTypeNettoyage(v: string | undefined): string {
    return v ? (LIBELLE_TYPE_NETTOYAGE as Record<string, string>)[v] ?? v : '';
  }

  libelleTarif(t: Tarif): string {
    const prix = t.prixauklo
      ? `(${t.prixauklo.toLocaleString('fr-FR')} FCFA/kg)`
      : t.prixunitaire
        ? `(${t.prixunitaire.toLocaleString('fr-FR')} FCFA/pièce)`
        : '';
    return `${t.nom ?? ''} ${t.typevetement ?? ''} ${prix}`.trim();
  }

  prixTarif(t: Tarif): number | undefined {
    return this.estAuPoids(t) ? t.prixauklo : t.prixunitaire;
  }

  /** Indique si un tarif est déjà dans le panier. */
  quantitePanier(t: Tarif): number {
    const l = this.lignes().find((x) => x.tarifId === t.idtarif);
    if (!l) return 0;
    return l.auPoids ? (l.poids ?? 0) : (l.quantite ?? 0);
  }

  ajouterAuPanier(t: Tarif): void {
    if (t.idtarif === undefined || t.idtarif === null) return;
    const auPoids = this.estAuPoids(t);
    const existante = this.lignes().find((x) => x.tarifId === t.idtarif);
    if (existante) {
      this.lignes.update((lignes) =>
        lignes.map((l) => {
          if (l.tarifId !== t.idtarif) return l;
          if (l.auPoids) {
            const p = Math.round(((l.poids ?? 0) + 0.5) * 10) / 10;
            return { ...l, poids: p };
          }
          return { ...l, quantite: (l.quantite ?? 0) + 1 };
        }),
      );
      return;
    }
    this.lignes.update((lignes) => [
      ...lignes,
      {
        tarifId: t.idtarif!,
        auPoids,
        poids: auPoids ? 1 : null,
        quantite: auPoids ? null : 1,
        typeNettoyage: t.typeNettoyage ?? 'LAVAGE_SIMPLE',
        description: '',
        photos: [],
      },
    ]);
  }

  incrementer(i: number): void {
    this.lignes.update((lignes) =>
      lignes.map((l, idx) => {
        if (idx !== i) return l;
        if (l.auPoids) {
          return { ...l, poids: Math.round(((l.poids ?? 0) + 0.5) * 10) / 10 };
        }
        return { ...l, quantite: (l.quantite ?? 0) + 1 };
      }),
    );
  }

  decrementer(i: number): void {
    const l = this.lignes()[i];
    if (!l) return;
    const valeur = l.auPoids ? (l.poids ?? 0) : (l.quantite ?? 0);
    const pas = l.auPoids ? 0.5 : 1;
    if (valeur - pas <= 0) {
      this.retirerLigne(i);
      return;
    }
    this.lignes.update((lignes) =>
      lignes.map((item, idx) => {
        if (idx !== i) return item;
        if (item.auPoids) {
          return { ...item, poids: Math.round((valeur - pas) * 10) / 10 };
        }
        return { ...item, quantite: valeur - pas };
      }),
    );
  }

  decrementerLigneTarif(tarifId: number): void {
    const i = this.lignes().findIndex((l) => l.tarifId === tarifId);
    if (i >= 0) this.decrementer(i);
  }

  basculerUnite(i: number): void {
    this.lignes.update((lignes) =>
      lignes.map((l, idx) => {
        if (idx !== i) return l;
        if (l.auPoids) {
          return { ...l, auPoids: false, quantite: l.quantite ?? 1, poids: null };
        }
        return { ...l, auPoids: true, poids: l.poids ?? 1, quantite: null };
      }),
    );
  }

  tarifDe(ligne: LigneFormulaire): Tarif | undefined {
    return this.tarifs().find((t) => t.idtarif === ligne.tarifId);
  }

  nomLigne(ligne: LigneFormulaire): string {
    const t = this.tarifDe(ligne);
    return t ? `${t.nom ?? ''} ${t.typevetement ?? ''}`.trim() : 'Article';
  }

  prixLigne(ligne: LigneFormulaire): number {
    const t = this.tarifDe(ligne);
    if (!t) return 0;
    return (ligne.auPoids ? t.prixauklo : t.prixunitaire) ?? 0;
  }

  montantLigne(l: LigneFormulaire): number {
    const t = this.tarifDe(l);
    if (!t) return 0;
    if (l.auPoids) {
      const prix = t.prixauklo;
      const poids = l.poids;
      if (prix === null || prix === undefined || poids === null || poids === undefined || poids <= 0) {
        return 0;
      }
      return prix * poids;
    }
    const prix = t.prixunitaire;
    const qte = l.quantite;
    if (prix === null || prix === undefined || qte === null || qte === undefined || qte <= 0) {
      return 0;
    }
    return prix * qte;
  }

  onPhotosSelectionnes(i: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichiers = input.files ? Array.from(input.files) : [];
    const valides: PhotoSelection[] = [];
    for (const f of fichiers) {
      if (!f.type.startsWith('image/')) {
        this.erreur.set('Seules les images sont acceptées.');
        continue;
      }
      if (f.size > 10 * 1024 * 1024) {
        this.erreur.set('Image trop volumineuse : maximum 10 Mo par image.');
        continue;
      }
      valides.push({ fichier: f, apercu: URL.createObjectURL(f) });
    }
    if (valides.length > 0) {
      this.lignes.update((l) => {
        const copie = [...l];
        copie[i] = { ...copie[i], photos: [...copie[i].photos, ...valides] };
        return copie;
      });
    }
    input.value = '';
  }

  retirerPhoto(i: number, idx: number): void {
    this.lignes.update((l) => {
      const copie = [...l];
      copie[i] = { ...copie[i], photos: copie[i].photos.filter((_, p) => p !== idx) };
      return copie;
    });
  }

  viderPanier(): void {
    this.lignes.set([]);
  }

  acompteValide(): boolean {
    return (this.acompte() ?? 0) > 0;
  }

  retirerLigne(i: number): void {
    this.lignes.update((l) => l.filter((_, idx) => idx !== i));
  }

  annuler(): void {
    this.router.navigate(['/commandes']);
  }

  chercherClient(): void {
    const q = this.recherchePhone().trim();
    if (!q) return;
    this.resultats.set([]);
    if (q.replace(/\D/g, '').length >= 8) {
      this.clients.rechercherParTelephone(q).subscribe({
        next: (c) => this.resultats.set([c]),
        error: () => this.resultats.set([]),
      });
    } else {
      this.clients.rechercherParNom(q).subscribe({
        next: (p) => this.resultats.set(p.content ?? []),
        error: () => this.resultats.set([]),
      });
    }
  }

  choisirClient(c: Client): void {
    this.clientSelectionne.set(c);
    this.resultats.set([]);
  }

  changerClient(): void {
    this.clientSelectionne.set(null);
  }

  enregistrer(): void {
    if (!this.clientSelectionne()) {
      this.erreur.set('Sélectionnez un client.');
      return;
    }
    if (this.lignes().length === 0) {
      this.erreur.set('Ajoutez au moins un article au panier.');
      return;
    }
    const lignesDTO: LigneCommandeDTO[] = [];
    for (const l of this.lignes()) {
      if (!l.tarifId) {
        this.erreur.set('Chaque ligne doit avoir un tarif.');
        return;
      }
      if (l.auPoids && (l.poids === null || l.poids <= 0)) {
        this.erreur.set('Renseignez un poids pour les lignes au poids.');
        return;
      }
      if (!l.auPoids && (l.quantite === null || l.quantite <= 0)) {
        this.erreur.set('Renseignez une quantité pour les lignes à la pièce.');
        return;
      }
      lignesDTO.push({
        tarifId: l.tarifId,
        poids: l.auPoids ? l.poids! : undefined,
        quantite: !l.auPoids ? l.quantite! : undefined,
        typeNettoyage: l.typeNettoyage as TypeNettoyageValue,
        description: l.description || undefined,
      });
    }

    if (this.montantTotal() <= 0) {
      this.erreur.set('Renseignez au moins un article avec un poids ou une quantité.');
      return;
    }
    if ((this.acompte() ?? 0) > this.montantTotal()) {
      this.erreur.set("L'acompte ne peut pas dépasser le montant total.");
      return;
    }

    const dto: CommandeRequestDTO = {
      clientId: this.clientSelectionne()!.id!,
      lignes: lignesDTO,
      dateRetraitPrevue: this.dateRetrait() || undefined,
      acompte: this.acompte() && this.acompte()! > 0 ? this.acompte()! : undefined,
      moyenAcompte: this.acompte() && this.acompte()! > 0 ? this.moyenAcompte() : undefined,
      messageAgent: this.messageAgent().trim() || undefined,
    };

    this.enregistrement.set(true);
    this.erreur.set('');
    this.commandes.creer(dto).subscribe({
      next: (cmd) => this.uploaderPhotos(cmd),
      error: (err) => {
        this.enregistrement.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  private uploaderPhotos(cmd: Commande): void {
    const uploads: Observable<boolean>[] = [];
    this.lignes().forEach((l, i) => {
      const idligne = cmd.lignes?.[i]?.idligne;
      if (!idligne) return;
      for (const p of l.photos) {
        uploads.push(
          this.photos.uploader(idligne, p.fichier, l.typeNettoyage).pipe(
            map(() => true),
            catchError(() => of(false))
          )
        );
      }
    });

    const naviguer = () => this.router.navigate(['/commandes', cmd.idcommande]);

    if (uploads.length === 0) {
      this.toasts.succes('Commande enregistrée.');
      naviguer();
      return;
    }

    forkJoin(uploads).subscribe({
      next: (resultats) => {
        const echecs = resultats.filter((ok) => !ok).length;
        if (echecs === 0) {
          this.toasts.succes('Commande enregistrée avec ses photos.');
        } else if (echecs === resultats.length) {
          this.toasts.erreur(
            "Commande enregistrée, mais aucune photo n'a pu être envoyée. Ajoutez-les depuis la fiche de la commande."
          );
        } else {
          this.toasts.erreur(
            `Commande enregistrée, mais ${echecs} photo(s) sur ${resultats.length} n'ont pas pu être envoyées.`
          );
        }
        naviguer();
      },
      error: () => {
        this.toasts.erreur(
          "Commande enregistrée, mais l'envoi des photos a échoué. Réessayez depuis la fiche de la commande."
        );
        naviguer();
      },
    });
  }
}
