import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Icon } from '../../shared/icon/icon';
import { ClientService } from '../../core/services/client.service';
import { TenantService } from '../../core/services/tenant.service';
import { Client } from '../../core/models/client.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-clients',
  imports: [FormsModule, Icon],
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
  readonly erreur = signal('');
  readonly message = signal('');

  readonly showForm = signal(false);
  readonly form = signal<Client>({ nom: '', telephone: '', ville: '', quartier: '' });
  readonly enregistrement = signal(false);

  constructor() {
    this.charger();
  }

  charger(): void {
    this.enChargement.set(true);
    this.service.lister(0, 50).subscribe({
      next: (p) => {
        this.clients.set(p.content ?? []);
        this.enChargement.set(false);
      },
      error: () => this.enChargement.set(false),
    });
  }

  resultats = signal<Client[]>([]);

  rechercher(): void {
    const q = this.recherche().trim();
    if (!q) {
      this.charger();
      return;
    }
    this.enChargement.set(true);
    if (q.replace(/\D/g, '').length >= 8) {
      this.service.rechercherParTelephone(q).subscribe({
        next: (c) => {
          this.resultats.set([c]);
          this.enChargement.set(false);
        },
        error: () => {
          this.resultats.set([]);
          this.enChargement.set(false);
        },
      });
    } else {
      this.service.rechercherParNom(q).subscribe({
        next: (p) => {
          this.resultats.set(p.content ?? []);
          this.enChargement.set(false);
        },
        error: () => this.enChargement.set(false),
      });
    }
  }

  effacerRecherche(): void {
    this.recherche.set('');
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

  voirCommandes(c: Client): void {
    this.router.navigate(['/commandes'], { queryParams: { client: c.id ?? undefined, nom: c.nom } });
  }
}