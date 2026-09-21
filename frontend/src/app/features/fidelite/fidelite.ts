import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../../shared/icon/icon';
import { Pagination } from '../../shared/pagination/pagination';
import { DateFrPipe } from '../../shared/pipes/date.fr.pipe';
import { FideliteService } from '../../core/services/fidelite.service';
import { ClientService } from '../../core/services/client.service';
import { CommandeService } from '../../core/services/commande.service';
import { TenantService } from '../../core/services/tenant.service';
import { Client } from '../../core/models/client.model';
import { CommandeDTO } from '../../core/models/commande.model';
import { HistoriquePoint, SoldeFideliteDTO } from '../../core/models/fidelite.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-fidelite',
  imports: [FormsModule, Icon, DateFrPipe, Pagination],
  templateUrl: './fidelite.html',
  styleUrl: '../_feature.scss',
})
export class Fidelite {
  private readonly service = inject(FideliteService);
  private readonly clients = inject(ClientService);
  private readonly commandes = inject(CommandeService);
  private readonly tenant = inject(TenantService);

  readonly estAdmin = this.tenant.estAdmin;

  readonly recherchePhone = signal('');
  readonly resultats = signal<Client[]>([]);
  readonly client = signal<Client | null>(null);

  readonly resultPage = signal(0);
  readonly resultTotalPages = signal(0);
  readonly rechercheNom = signal('');

  readonly enChargement = signal(true);
  readonly solde = signal<SoldeFideliteDTO | null>(null);
  readonly historique = signal<HistoriquePoint[]>([]);
  readonly commandesClient = signal<CommandeDTO[]>([]);

  readonly commandeId = signal<number | null>(null);
  readonly nbPoints = signal<number>(0);
  readonly nbPointsCredit = signal<number>(0);
  readonly descCredit = signal('');

  readonly enCoursUtilisation = signal(false);
  readonly enCoursCredit = signal(false);
  readonly message = signal('');
  readonly erreur = signal('');

  chercher(): void {
    const q = this.recherchePhone().trim();
    if (!q) return;
    this.resultats.set([]);
    this.resultPage.set(0);
    if (q.replace(/\D/g, '').length >= 8) {
      this.rechercheNom.set('');
      this.clients.rechercherParTelephone(q).subscribe({
        next: (c) => {
          this.resultats.set([c]);
          this.resultTotalPages.set(1);
        },
        error: () => {
          this.resultats.set([]);
          this.resultTotalPages.set(1);
        },
      });
    } else {
      this.rechercheNom.set(q);
      this.rechercherNomPage(0);
    }
  }

  changerResultPage(p: number): void {
    const q = this.rechercheNom();
    if (!q) return;
    this.resultPage.set(p);
    this.rechercherNomPage(p);
  }

  private rechercherNomPage(page: number): void {
    this.clients.rechercherParNom(this.rechercheNom(), page, 10).subscribe({
      next: (p) => {
        this.resultats.set(p.content ?? []);
        this.resultTotalPages.set(p.totalPages ?? 1);
      },
      error: () => {
        this.resultats.set([]);
        this.resultTotalPages.set(1);
      },
    });
  }

  choisirClient(c: Client): void {
    if (!c.id) return;
    this.client.set(c);
    this.charger(c.id);
  }

  changerClient(): void {
    this.client.set(null);
    this.resultats.set([]);
    this.commandeId.set(null);
    this.solde.set(null);
    this.historique.set([]);
    this.commandesClient.set([]);
  }

  private charger(id: number): void {
    this.enChargement.set(true);
    this.service.solde(id).subscribe({
      next: (s) => this.solde.set(s),
      error: () => this.solde.set(null),
    });
    this.service.historique(id).subscribe({
      next: (h) => this.historique.set(h ?? []),
      error: () => this.historique.set([]),
    });
    this.commandes.parClient(id).subscribe({
      next: (liste) => {
        this.commandesClient.set((liste ?? []).filter((c) => (c.resteAPayer ?? 0) > 0));
        this.enChargement.set(false);
      },
      error: () => {
        this.commandesClient.set([]);
        this.enChargement.set(false);
      },
    });
  }

  utiliser(): void {
    const c = this.client();
    const cmdId = this.commandeId();
    if (!c || !c.id) return;
    if (c.id == null || !cmdId || (this.nbPoints() ?? 0) <= 0) {
      this.erreur.set('Choisissez une commande et entrez un nombre de points.');
      return;
    }
    this.enCoursUtilisation.set(true);
    const id = c.id;
    this.service.utiliser({ clientId: id, commandeId: cmdId, nbPoints: this.nbPoints() ?? 0 }).subscribe({
      next: (rep) => {
        this.enCoursUtilisation.set(false);
        this.message.set(`Remise appliquée : ${(rep.remise ?? 0).toLocaleString('fr-FR')} FCFA (${rep.nbPointsUtilises ?? 0} pts).`);
        this.nbPoints.set(0);
        this.commandeId.set(null);
        this.charger(id);
      },
      error: (err) => {
        this.enCoursUtilisation.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  crediter(): void {
    const c = this.client();
    if (!c || !c.id || (this.nbPointsCredit() ?? 0) <= 0) {
      this.erreur.set('Entrez un nombre de points positif.');
      return;
    }
    this.enCoursCredit.set(true);
    const id = c.id;
    this.service
      .crediter({ clientId: id, nbPoints: this.nbPointsCredit() ?? 0, description: this.descCredit() || undefined })
      .subscribe({
        next: () => {
          this.enCoursCredit.set(false);
          this.message.set('Points crédités.');
          this.nbPointsCredit.set(0);
          this.descCredit.set('');
          this.charger(id);
        },
        error: (err) => {
          this.enCoursCredit.set(false);
          this.erreur.set(extraireMessageErreur(err));
        },
      });
  }

  libelleType(type: string): string {
    switch (type) {
      case 'GAGNE':
        return 'Gagne';
      case 'UTILISE':
        return 'Utilisé';
      case 'CREDIT':
        return 'Crédit manuel';
      case 'EXPIRE':
        return 'Expiré';
      default:
        return type;
    }
  }
}