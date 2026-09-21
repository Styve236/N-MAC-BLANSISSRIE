import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Icon } from '../../shared/icon/icon';
import { Pagination } from '../../shared/pagination/pagination';
import { ClientService } from '../../core/services/client.service';
import { TenantService } from '../../core/services/tenant.service';
import { Client } from '../../core/models/client.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-clients',
  imports: [FormsModule, Icon, Pagination],
  templateUrl: './clients.html',
  styleUrl: '../_feature.scss',
})
export class Clients {
  private readonly service = inject(ClientService);
  private readonly tenant = inject(TenantService);
  private readonly router = inject(Router);

  readonly peutEcrire = this.tenant.peutEcrire;

  readonly clients = signal<Client[]>([]);
  readonly enChargement = signal(true);
  readonly recherche = signal('');
  readonly totalPages = signal(0);
  readonly page = signal(0);
  readonly taille = signal(20);
  readonly erreur = signal('');
  readonly message = signal('');

  readonly showForm = signal(false);
  readonly form = signal<Client>({ nom: '', telephone: '', ville: '', quartier: '' });
  readonly enregistrement = signal(false);
  readonly suppressionId = signal<number | null>(null);

  constructor() {
    this.charger();
  }

  charger(): void {
    this.enChargement.set(true);
    this.service.lister(this.page(), this.taille()).subscribe({
      next: (p) => {
        this.clients.set(p.content ?? []);
        this.totalPages.set(p.totalPages ?? 0);
        this.enChargement.set(false);
      },
      error: () => this.enChargement.set(false),
    });
  }

  rechercher(): void {
    const q = this.recherche().trim();
    if (!q) {
      this.charger();
      return;
    }
    this.page.set(0);
    this.enChargement.set(true);
    if (q.replace(/\D/g, '').length >= 8) {
      this.service.rechercherParTelephone(q).subscribe({
        next: (c) => {
          this.clients.set([c]);
          this.totalPages.set(1);
          this.enChargement.set(false);
        },
        error: () => {
          this.clients.set([]);
          this.totalPages.set(1);
          this.enChargement.set(false);
        },
      });
    } else {
      this.service.rechercherParNom(q, this.page(), this.taille()).subscribe({
        next: (p) => {
          this.clients.set(p.content ?? []);
          this.totalPages.set(p.totalPages ?? 0);
          this.enChargement.set(false);
        },
        error: () => {
          this.clients.set([]);
          this.totalPages.set(1);
          this.enChargement.set(false);
        },
      });
    }
  }

  changerPage(p: number): void {
    this.page.set(p);
    if (this.recherche().trim()) {
      this.rechercher();
    } else {
      this.charger();
    }
  }

  changerTaille(t: number): void {
    this.taille.set(t);
    this.page.set(0);
    if (this.recherche().trim()) {
      this.rechercher();
    } else {
      this.charger();
    }
  }

  effacerRecherche(): void {
    this.recherche.set('');
    this.page.set(0);
    this.charger();
  }

  ouvrirNouveau(): void {
    this.form.set({ nom: '', telephone: '', ville: '', quartier: '' });
    this.showForm.set(true);
    this.erreur.set('');
  }

  enregistrer(): void {
    const f = this.form();
    if (!f.nom || !f.telephone) {
      this.erreur.set('Le nom et le téléphone sont obligatoires.');
      return;
    }
    this.enregistrement.set(true);
    this.service.creer(f).subscribe({
      next: () => {
        this.enregistrement.set(false);
        this.showForm.set(false);
        this.message.set('Client enregistré.');
        this.charger();
      },
      error: (err) => {
        this.enregistrement.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  annuler(): void {
    this.showForm.set(false);
    this.erreur.set('');
  }

  supprimer(c: Client): void {
    if (!c.id) return;
    if (!window.confirm(`Supprimer le client « ${c.nom} » ?`)) return;
    this.suppressionId.set(c.id);
    this.erreur.set('');
    this.service.supprimer(c.id).subscribe({
      next: () => {
        this.suppressionId.set(null);
        this.message.set('Client supprimé.');
        this.charger();
      },
      error: (err) => {
        this.suppressionId.set(null);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  voirCommandes(c: Client): void {
    this.router.navigate(['/commandes'], { queryParams: { client: c.id ?? undefined, nom: c.nom } });
  }
}