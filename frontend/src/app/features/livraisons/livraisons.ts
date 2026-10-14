import { Component, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { DateFrPipe } from '../../shared/pipes/date.fr.pipe';
import { Icon } from '../../shared/icon/icon';
import { LIBELLE_STATUT_LIVRAISON } from '../../shared/libelles';
import { StatutLivraison, STATUT_LIVRAISON } from '../../core/config/constants';
import { LivraisonService } from '../../core/services/livraison.service';
import { UserService } from '../../core/services/user.service';
import { TenantService } from '../../core/services/tenant.service';
import { LivraisonDTO, LivraisonRequestDTO } from '../../core/models/livraison.model';
import { UserDTO } from '../../core/models/users.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-livraisons',
  imports: [FormsModule, CfaPipe, DateFrPipe, Icon],
  templateUrl: './livraisons.html',
  styleUrls: ['../_feature.scss', './livraisons.scss'],
})
export class Livraisons {
  private readonly service = inject(LivraisonService);
  private readonly users = inject(UserService);
  private readonly tenant = inject(TenantService);

  readonly estLivreur = this.tenant.estLivreur;
  readonly estAdmin = this.tenant.estAdmin;
  readonly nomCourant = this.tenant.nom;

  readonly livraisons = signal<LivraisonDTO[]>([]);
  readonly livreurs = signal<UserDTO[]>([]);
  readonly livreursOk = signal(false);
  readonly enChargement = signal(true);
  readonly erreur = signal('');
  readonly message = signal('');

  readonly filtre = signal<'all' | 'pending' | 'completed'>('all');

  readonly statuts = Object.values(STATUT_LIVRAISON) as StatutLivraison[];

  readonly assignId = signal<number | null>(null);
  readonly assignForm = signal<LivraisonRequestDTO>({ statut: 'A_LIVRER' });
  readonly enCoursAssign = signal(false);

  readonly enCoursStatut = signal(false);
  readonly enCoursMasquageId = signal<number | null>(null);

  readonly validation = signal<LivraisonDTO | null>(null);
  readonly moyenValidation = signal<string>('ESPECES');

  readonly enAttente = computed(() =>
    this.livraisons().filter((l) => l.statutLivraison === 'A_LIVRER' || l.statutLivraison === 'EN_COURS'),
  );
  readonly terminees = computed(() => this.livraisons().filter((l) => l.statutLivraison === 'LIVREE'));

  readonly missionsFiltrees = computed(() => {
    const f = this.filtre();
    if (f === 'pending') return this.enAttente();
    if (f === 'completed') return this.terminees();
    return this.livraisons();
  });

  readonly totalEncaisse = computed(() =>
    this.terminees().reduce((s, l) => s + (l.montantPaye ?? 0), 0),
  );

  constructor() {
    this.charger();
    this.chargerLivreurs();
  }

  charger(): void {
    this.enChargement.set(true);
    this.erreur.set('');
    const appel = this.estLivreur() ? this.service.mesLivraisons() : this.service.lister();
    appel.subscribe({
      next: (l) => {
        this.livraisons.set(l ?? []);
        this.enChargement.set(false);
      },
      error: (err) => {
        this.enChargement.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  chargerLivreurs(): void {
    if (this.estLivreur()) return;
    this.users.listerLivreurs().subscribe({
      next: (u) => {
        this.livreurs.set(u.filter((x) => x.role === 'LIVREUR' && x.actif));
        this.livreursOk.set(true);
      },
      error: () => {
        this.livreursOk.set(false);
      },
    });
  }

  montantARecevoir(l: LivraisonDTO): number {
    return Math.max(0, (l.montantTotal ?? 0) - (l.montantPaye ?? 0));
  }

  libelleStatut(s: string | undefined): string {
    return (LIBELLE_STATUT_LIVRAISON as Record<string, string>)[s ?? ''] ?? (s ?? '');
  }

  lienTel(l: LivraisonDTO): string {
    return `tel:${(l.clientTelephone ?? '').replace(/\s/g, '')}`;
  }

  lienWhatsApp(l: LivraisonDTO): string {
    const tel = (l.clientTelephone ?? '').replace(/[^0-9]/g, '');
    const texte = encodeURIComponent(
      `Bonjour ${l.clientNom ?? ''}, c'est votre coursier N MAC Blanchisserie (Douala PK8) pour votre commande ${l.numeroTicket ?? ''}. Je suis en route pour votre adresse.`,
    );
    return `https://wa.me/${tel}?text=${texte}`;
  }

  demarrer(l: LivraisonDTO): void {
    if (!l.idcommande) return;
    this.changerStatut(l.idcommande, 'EN_COURS', 'Itinéraire démarré.');
  }

  ouvrirValidation(l: LivraisonDTO): void {
    this.moyenValidation.set('ESPECES');
    this.validation.set(l);
  }

  fermerValidation(): void {
    this.validation.set(null);
  }

  confirmerValidation(): void {
    const l = this.validation();
    if (!l?.idcommande) return;
    this.enCoursStatut.set(true);
    this.erreur.set('');
    this.service.changerStatut(l.idcommande, 'LIVREE').subscribe({
      next: () => {
        this.enCoursStatut.set(false);
        this.validation.set(null);
        this.message.set(`Livraison ${l.numeroTicket} validée.`);
        this.charger();
      },
      error: (err) => {
        this.enCoursStatut.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  private changerStatut(id: number, statut: string, msg: string): void {
    this.enCoursStatut.set(true);
    this.erreur.set('');
    this.service.changerStatut(id, statut).subscribe({
      next: () => {
        this.enCoursStatut.set(false);
        this.message.set(msg);
        this.charger();
      },
      error: (err) => {
        this.enCoursStatut.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  ouvrirAssign(l: LivraisonDTO): void {
    this.assignId.set(l.idcommande ?? null);
    this.assignForm.set({
      adresseLivraison: l.adresseLivraison ?? '',
      livreurId: l.livreurId,
      dateLivraisonPrevue: l.dateLivraisonPrevue,
      fraisLivraison: l.fraisLivraison ?? 0,
      statut: 'A_LIVRER',
    });
    this.erreur.set('');
  }

  annulerAssign(): void {
    this.assignId.set(null);
  }

  assigner(): void {
    const id = this.assignId();
    if (!id) return;
    if (!this.assignForm().adresseLivraison) {
      this.erreur.set("L'adresse de livraison est obligatoire.");
      return;
    }
    if (!this.assignForm().livreurId && !this.livreursOk()) {
      this.erreur.set('Un livreur est obligatoire.');
      return;
    }
    this.enCoursAssign.set(true);
    this.erreur.set('');
    this.service.assigner(id, this.assignForm()).subscribe({
      next: () => {
        this.enCoursAssign.set(false);
        this.assignId.set(null);
        this.message.set('Livraison assignée.');
        this.charger();
      },
      error: (err) => {
        this.enCoursAssign.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  masquer(l: LivraisonDTO): void {
    if (!l.idcommande) return;
    this.enCoursMasquageId.set(l.idcommande);
    this.erreur.set('');
    this.service.masquer(l.idcommande).subscribe({
      next: () => {
        this.enCoursMasquageId.set(null);
        this.message.set(`Livraison ${l.numeroTicket} retirée de votre liste.`);
        this.charger();
      },
      error: (err) => {
        this.enCoursMasquageId.set(null);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }
}
