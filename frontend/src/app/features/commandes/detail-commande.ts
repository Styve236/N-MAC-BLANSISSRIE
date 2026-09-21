import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { DateFrPipe } from '../../shared/pipes/date.fr.pipe';
import { Icon } from '../../shared/icon/icon';
import { StatutBadge } from '../../shared/statut-badge/statut-badge';
import {
  LIBELLE_STATUT_COMMANDE,
  LIBELLE_MOYEN_PAIEMENT,
  moyenPaiementOptions,
} from '../../shared/libelles';
import { StatutCommande, StatutLivraison, STATUT_COMMANDE } from '../../core/config/constants';
import { CommandeService } from '../../core/services/commande.service';
import { PaiementService } from '../../core/services/paiement.service';
import { PhotoService } from '../../core/services/photo.service';
import { ExportService, telechargerBlob } from '../../core/services/export.service';
import { LivraisonService } from '../../core/services/livraison.service';
import { UserService } from '../../core/services/user.service';
import { TenantService } from '../../core/services/tenant.service';
import { Commande, LigneCommandeLegere, Photo, StatutPaiementDTO } from '../../core/models/commande.model';
import { PaiementDTO, PaiementResponseDTO } from '../../core/models/paiement.model';
import { LivraisonRequestDTO } from '../../core/models/livraison.model';
import { UserDTO } from '../../core/models/users.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-detail-commande',
  imports: [FormsModule, CfaPipe, DateFrPipe, Icon, StatutBadge],
  templateUrl: './detail-commande.html',
  styleUrl: '../_feature.scss',
})
export class DetailCommande {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cmd = inject(CommandeService);
  private readonly paiement = inject(PaiementService);
  private readonly photos = inject(PhotoService);
  private readonly exports = inject(ExportService);
  private readonly livraisons = inject(LivraisonService);
  private readonly users = inject(UserService);
  private readonly tenant = inject(TenantService);

  readonly peutPayer = this.tenant.peutEcrire;
  readonly peutChangerStatut = this.tenant.peutGererStatuts;

  readonly commande = signal<Commande | null>(null);
  readonly statutPaiement = signal<StatutPaiementDTO | null>(null);
  readonly photosParLigne = signal<Record<number, Photo[]>>({});
  readonly enChargement = signal(true);
  readonly erreur = signal('');
  readonly message = signal('');

  readonly nouveauStatut = signal('');
  readonly moyensPaiement = moyenPaiementOptions();
  readonly paiementForm = signal<PaiementDTO>({ montant: 0, moyenPaiement: 'ESPECES' });
  readonly enCoursStatut = signal(false);
  readonly enCoursPaiement = signal(false);

  readonly afficherFormLivraison = signal(false);
  readonly livreurs = signal<UserDTO[]>([]);
  readonly adresseLivraison = signal('');
  readonly livreurId = signal<number | null>(null);
  readonly fraisLivraison = signal<number>(0);
  readonly dateLivraisonPrevue = signal('');
  readonly enCoursLivraison = signal(false);

  readonly commandes = Object.values(STATUT_COMMANDE) as StatutCommande[];

  constructor() {
    this.charger();
  }

  private charger(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.enChargement.set(true);
    this.cmd.obtenir(id).subscribe({
      next: (c) => {
        this.commande.set(c);
        this.nouveauStatut.set(c.statut ?? '');
        this.chargerPhotos();
        this.chargerPaiement(id);
        this.chargerLivreurs();
      },
      error: () => {
        this.enChargement.set(false);
        this.router.navigate(['/commandes']);
      },
    });
  }

  private chargerPhotos(): void {
    const ids = (this.commande()?.lignes ?? [])
      .map((l) => l.idligne)
      .filter((id): id is number => !!id);
    if (ids.length === 0) return;
    for (const id of ids) {
      this.photos.lister(id).subscribe({
        next: (liste) =>
          this.photosParLigne.update((m) => ({ ...m, [id]: liste ?? [] })),
        error: () =>
          this.photosParLigne.update((m) => ({ ...m, [id]: [] })),
      });
    }
  }

  photosDe(l: LigneCommandeLegere | undefined): Photo[] {
    return l?.idligne != null ? (this.photosParLigne()[l.idligne] ?? []) : [];
  }

  fichierUrl(idphoto: number): string {
    return this.photos.fichierUrl(idphoto);
  }

  supprimerPhoto(idphoto: number): void {
    if (!window.confirm('Supprimer cette photo ?')) return;
    this.photos.supprimer(idphoto).subscribe({
      next: () => {
        this.photosParLigne.update((m) => {
          const nouveau: Record<number, Photo[]> = {};
          for (const [cle, liste] of Object.entries(m)) {
            nouveau[Number(cle)] = liste.filter((p) => p.idphoto !== idphoto);
          }
          return nouveau;
        });
      },
      error: (err) => this.erreur.set(extraireMessageErreur(err)),
    });
  }

  private chargerLivreurs(): void {
    this.users.listerLivreurs().subscribe({
      next: (liste) => this.livreurs.set(liste ?? []),
      error: () => undefined,
    });
  }

  private chargerPaiement(id: number): void {
    this.paiement.statutPaiement(id).subscribe({
      next: (sp) => {
        this.statutPaiement.set(sp);
        this.enChargement.set(false);
      },
      error: () => this.enChargement.set(false),
    });
  }

  appliquerStatut(): void {
    const c = this.commande();
    if (!c) return;
    const statut = this.nouveauStatut();
    if (statut === c.statut) return;
    this.enCoursStatut.set(true);
    this.erreur.set('');

    if (statut === 'PRET') {
      const livreursActifs = this.livreurs();
      if (livreursActifs.length === 0) {
        this.enCoursStatut.set(false);
        this.erreur.set('Aucun livreur actif : la commande est marquée prête sans livraison. Créez un compte LIVREUR.');
        this.cmd.marquerPret(c.idcommande!).subscribe({
          next: (updated) => {
            this.commande.set(updated);
            this.message.set('Statut mis à jour (aucun livreur — livraison non créée).');
          },
          error: (err) => this.erreur.set(extraireMessageErreur(err)),
        });
        return;
      }

      const lid = this.livreurId() ?? livreursActifs[0].idusers ?? null;
      const adr = this.adresseLivraison().trim() || this.adresseClient();
      if (!lid) {
        this.enCoursStatut.set(false);
        this.erreur.set('Aucun livreur sélectionné.');
        return;
      }

      this.cmd.marquerPret(c.idcommande!).subscribe({
        next: (updated) => {
          this.commande.set(updated);
          const dto: LivraisonRequestDTO = {
            adresseLivraison: adr,
            livreurId: lid,
            fraisLivraison: this.fraisLivraison() || 0,
            dateLivraisonPrevue: this.dateLivraisonPrevue() || undefined,
          };
          this.livraisons.assigner(c.idcommande!, dto).subscribe({
            next: () => {
              this.enCoursStatut.set(false);
              this.message.set('Commande prête et livraison envoyée au livreur.');
              this.charger();
            },
            error: (err) => {
              this.enCoursStatut.set(false);
              this.message.set('Statut mis à jour, mais échec assignation livraison.');
              this.erreur.set(extraireMessageErreur(err));
              this.charger();
            },
          });
        },
        error: (err) => {
          this.enCoursStatut.set(false);
          this.erreur.set(extraireMessageErreur(err));
        },
      });
      return;
    }

    const appel$ = statut === 'PRET'
      ? this.cmd.marquerPret(c.idcommande!)
      : this.cmd.changerStatut(c.idcommande!, statut);

    appel$.subscribe({
      next: (updated) => {
        this.commande.set(updated);
        this.enCoursStatut.set(false);
        this.message.set('Statut mis à jour.');
      },
      error: (err) => {
        this.enCoursStatut.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  adresseClient(): string {
    const cl = this.commande()?.client;
    const ville = cl?.ville?.trim();
    const quartier = cl?.quartier?.trim();
    if (ville && quartier) return `${quartier}, ${ville}`;
    if (quartier) return quartier;
    if (ville) return ville;
    return 'Adresse client à confirmer';
  }

  livreurActifLabel(): string {
    const id = this.livreurId() ?? this.livreurs()[0]?.idusers;
    return this.livreurs().find((l) => l.idusers === id)?.nom ?? 'le livreur disponible';
  }

  payer(): void {
    const c = this.commande();
    const form = this.paiementForm();
    if (!c || form.montant <= 0) {
      this.erreur.set('Montant invalide.');
      return;
    }
    this.enCoursPaiement.set(true);
    this.erreur.set('');
    this.paiement.payer(c.idcommande!, form).subscribe({
      next: (res) => {
        this.enCoursPaiement.set(false);
        this.message.set(`Paiement enregistré : ${(res.montant ?? 0).toLocaleString('fr-FR')} FCFA (${this.libelleMoyen(res.moyenPaiement ?? '')}).`);
        this.paiementForm.set({ montant: 0, moyenPaiement: 'ESPECES' });
        this.charger();
      },
      error: (err) => {
        this.enCoursPaiement.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  envoyerRecu(): void {
    const id = this.commande()?.idcommande;
    if (!id) return;
    this.cmd.envoyerRecu(id).subscribe({
      next: () => this.message.set('Reçu envoyé par SMS.'),
      error: (err) => this.erreur.set(extraireMessageErreur(err)),
    });
  }

  retour(): void {
    this.router.navigate(['/commandes']);
  }

  telechargerRecuPdf(): void {
    const id = this.commande()?.idcommande;
    if (!id) return;
    this.exports.recuCommande(id).subscribe({
      next: (blob) => telechargerBlob(blob, `recu-commande-${id}.pdf`),
      error: (err) => this.erreur.set(extraireMessageErreur(err)),
    });
  }

  libelleStatut(s: string): string {
    return LIBELLE_STATUT_COMMANDE[s as StatutCommande] ?? s;
  }

  libelleMoyen(m: string): string {
    return (LIBELLE_MOYEN_PAIEMENT as Record<string, string>)[m] ?? m;
  }
}