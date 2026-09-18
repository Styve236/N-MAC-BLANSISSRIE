import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { DateFrPipe } from '../../shared/pipes/date.fr.pipe';
import { Icon } from '../../shared/icon/icon';
import { LIBELLE_TYPE_NETTOYAGE, LIBELLE_STATUT_COMMANDE, LIBELLE_MOYEN_PAIEMENT } from '../../shared/libelles';
import { StatistiquesService } from '../../core/services/statistiques.service';
import { ExportService, telechargerBlob } from '../../core/services/export.service';
import { SmsService } from '../../core/services/sms.service';
import { StatistiquesDTO, TopClientDTO, TopPrestationDTO } from '../../core/models/statistiques.model';
import { SmsNotificationDTO } from '../../core/models/sms.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-rapports',
  imports: [FormsModule, CfaPipe, DateFrPipe, Icon],
  templateUrl: './rapports.html',
  styleUrl: '../_feature.scss',
})
export class Rapports {
  private readonly stats = inject(StatistiquesService);
  private readonly exports = inject(ExportService);
  private readonly sms = inject(SmsService);

  readonly data = signal<StatistiquesDTO | null>(null);
  readonly smsListe = signal<SmsNotificationDTO[]>([]);
  readonly enChargement = signal(true);
  readonly erreur = signal('');
  readonly message = signal('');

  readonly debut = signal(new Date().toISOString().slice(0, 10));
  readonly fin = signal(new Date().toISOString().slice(0, 10));
  readonly enCoursExport = signal(false);

  constructor() {
    this.stats.synthese().subscribe({
      next: (s) => {
        this.data.set(s);
        this.enChargement.set(false);
      },
      error: () => this.enChargement.set(false),
    });
    this.sms.lister().subscribe({
      next: (l) => this.smsListe.set(l ?? []),
      error: () => undefined,
    });
  }

  commandesParStatut(): { statut: string; count: number }[] {
    return Object.entries(this.data()?.commandesParStatut ?? {}).map(([statut, count]) => ({
      statut,
      count,
    }));
  }

  encaissementsParMoyen(): { moyen: string; montant: number }[] {
    return Object.entries(this.data()?.encaissementParMoyen ?? {}).map(([moyen, montant]) => ({
      moyen,
      montant,
    }));
  }

  telecharger(exportType: 'commandes' | 'paiements' | 'caisse' | 'inventaire' | 'fidelite', format: 'pdf' | 'csv'): void {
    this.enCoursExport.set(true);
    this.erreur.set('');
    let obs;
    const d = this.debut();
    const f = this.fin();
    switch (exportType) {
      case 'commandes':
        obs = this.exports.commandes(d, f, format);
        break;
      case 'paiements':
        obs = this.exports.paiements(d, f, format);
        break;
      case 'caisse':
        obs = this.exports.caisse(format);
        break;
      case 'inventaire':
        obs = this.exports.inventaire(format);
        break;
      case 'fidelite':
        obs = this.exports.fidelite(format);
        break;
    }
    obs.subscribe({
      next: (blob) => {
        telechargerBlob(blob, `${exportType}.${format === 'pdf' ? 'pdf' : 'csv'}`);
        this.enCoursExport.set(false);
        this.message.set(`Export ${exportType} téléchargé.`);
      },
      error: (err) => {
        this.enCoursExport.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  libelleStatut(s: string): string {
    return (LIBELLE_STATUT_COMMANDE as Record<string, string>)[s] ?? s;
  }

  libelleMoyen(m: string): string {
    return (LIBELLE_MOYEN_PAIEMENT as Record<string, string>)[m] ?? m;
  }
}