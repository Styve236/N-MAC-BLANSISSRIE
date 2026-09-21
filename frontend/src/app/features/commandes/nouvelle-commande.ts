import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, Observable } from 'rxjs';
import { Icon } from '../../shared/icon/icon';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { moyenPaiementOptions, typeNettoyageOptions } from '../../shared/libelles';
import { ClientService } from '../../core/services/client.service';
import { TarifService } from '../../core/services/tarif.service';
import { CommandeService } from '../../core/services/commande.service';
import { PhotoService } from '../../core/services/photo.service';
import { TypeNettoyage as TypeNettoyageValue } from '../../core/config/constants';
import { Client } from '../../core/models/client.model';
import { Commande, CommandeRequestDTO, LigneCommandeDTO, Photo, Tarif } from '../../core/models/commande.model';
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
  tarif?: Tarif;
  photos: PhotoSelection[];
}

@Component({
  selector: 'app-nouvelle-commande',
  imports: [FormsModule, Icon, CfaPipe],
  templateUrl: './nouvelle-commande.html',
  styleUrl: '../_feature.scss',
})
export class NouvelleCommande {
  private readonly clients = inject(ClientService);
  private readonly tarifsSvc = inject(TarifService);
  private readonly commandes = inject(CommandeService);
  private readonly photos = inject(PhotoService);
  private readonly router = inject(Router);

  readonly typesNettoyage = typeNettoyageOptions();
  readonly moyensPaiement = moyenPaiementOptions();

  readonly tarifs = signal<Tarif[]>([]);
  readonly enChargementTarifs = signal(true);

  readonly recherchePhone = signal('');
  readonly resultats = signal<Client[]>([]);
  readonly clientSelectionne = signal<Client | null>(null);

  readonly lignes = signal<LigneFormulaire[]>([this.ligneVide()]);
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

  constructor() {
    this.tarifsSvc.actifs().subscribe({
      next: (t) => {
        this.tarifs.set(t);
        this.enChargementTarifs.set(false);
      },
      error: () => this.enChargementTarifs.set(false),
    });
  }

  ligneVide(): LigneFormulaire {
    return { tarifId: null, auPoids: true, poids: null, quantite: null, typeNettoyage: 'LAVAGE_SIMPLE', description: '', photos: [] };
  }

  ajouterLigne(): void {
    this.lignes.update((l) => [...l, this.ligneVide()]);
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

  acompteValide(): boolean {
    return (this.acompte() ?? 0) > 0;
  }

  retirerLigne(i: number): void {
    this.lignes.update((l) => l.filter((_, idx) => idx !== i));
  }

  tarifDe(ligne: LigneFormulaire): Tarif | undefined {
    return this.tarifs().find((t) => t.idtarif === ligne.tarifId);
  }

  libelleTarif(t: Tarif): string {
    const prix = t.prixauklo
      ? `(${t.prixauklo.toLocaleString('fr-FR')} FCFA/kg)`
      : t.prixunitaire
        ? `(${t.prixunitaire.toLocaleString('fr-FR')} FCFA/pièce)`
        : '';
    return `${t.nom ?? ''} ${t.typevetement ?? ''} ${prix}`.trim();
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
    const uploads: Observable<Photo>[] = [];
    this.lignes().forEach((l, i) => {
      const idligne = cmd.lignes?.[i]?.idligne;
      if (!idligne) return;
      for (const p of l.photos) {
        uploads.push(this.photos.uploader(idligne, p.fichier, l.typeNettoyage));
      }
    });
    if (uploads.length === 0) {
      this.router.navigate(['/commandes', cmd.idcommande]);
      return;
    }
    forkJoin(uploads).subscribe({
      next: () => this.router.navigate(['/commandes', cmd.idcommande]),
      error: () => this.router.navigate(['/commandes', cmd.idcommande]),
    });
  }
}