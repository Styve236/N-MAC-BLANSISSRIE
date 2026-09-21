import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PercentPipe } from '@angular/common';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { DateFrPipe } from '../../shared/pipes/date.fr.pipe';
import { Icon } from '../../shared/icon/icon';
import { Pagination } from '../../shared/pagination/pagination';
import { LIBELLE_TYPE_NETTOYAGE, LIBELLE_STATUT_COMMANDE, LIBELLE_MOYEN_PAIEMENT } from '../../shared/libelles';
import { StatistiquesService } from '../../core/services/statistiques.service';
import { ExportService, telechargerBlob } from '../../core/services/export.service';
import { SmsService } from '../../core/services/sms.service';
import { StatistiquesDTO } from '../../core/models/statistiques.model';
import { SmsNotificationDTO } from '../../core/models/sms.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';
import {
  SegmentAnneau,
  construireAnneau,
  CIRCONFERENCE,
  COULEUR_STATUT,
  COULEUR_MOYEN,
} from '../../shared/lib/anneau.util';

const COULEUR_NETTOYAGE: Record<string, string> = {
  NETTOYAGE_A_SEC: 'var(--teal)',
  LAUNDRY: 'var(--indigo)',
  LAVAGE_SIMPLE: 'var(--indigo)',
  REPASSAGE: 'var(--amber)',
};

interface Kpi {
  icon: string;
  label: string;
  valeur: number;
  cfa: boolean;
  type: 'revenu' | 'commandes' | 'clients' | 'impaye';
}

interface LigneStatut {
  cle: string;
  statut: string;
  count: number;
  couleur: string;
}

interface LigneMoyen {
  cle: string;
  moyen: string;
  montant: number;
  couleur: string;
}

interface LigneTop {
  cle: string;
  rang: number;
  rangClasse: string;
  nom: string;
  annexe: string;
  total: number;
  cfa: boolean;
  pct: number;
  couleur: string;
}

@Component({
  selector: 'app-rapports',
  imports: [FormsModule, CfaPipe, DateFrPipe, Icon, PercentPipe, Pagination],
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
  readonly anime = signal(false);

  readonly debut = signal(new Date().toISOString().slice(0, 10));
  readonly fin = signal(new Date().toISOString().slice(0, 10));
  readonly enCoursExport = signal(false);

  constructor() {
    this.stats.synthese().subscribe({
      next: (s) => {
        this.data.set(s);
        this.enChargement.set(false);
        setTimeout(() => this.anime.set(true), 60);
      },
      error: () => this.enChargement.set(false),
    });
    this.sms.lister().subscribe({
      next: (l) => this.smsListe.set(l ?? []),
      error: () => undefined,
    });
  }

  dasharraySegment(seg: SegmentAnneau): string {
    return this.anime() ? seg.dasharray : `0 ${CIRCONFERENCE}`;
  }

  kpis(): Kpi[] {
    const d = this.data();
    if (!d) return [];
    return [
      { icon: 'caisse', label: 'CA du jour', valeur: d.chiffreAffairesJour ?? 0, cfa: true, type: 'revenu' },
      { icon: 'caisse', label: 'CA du mois', valeur: d.chiffreAffairesMois ?? 0, cfa: true, type: 'revenu' },
      { icon: 'caisse', label: 'Total encaissé', valeur: d.totalEncaisse ?? 0, cfa: true, type: 'revenu' },
      { icon: 'commandes', label: 'Commandes aujourd’hui', valeur: d.commandesAujourdhui ?? 0, cfa: false, type: 'commandes' },
      { icon: 'commandes', label: 'Total commandes', valeur: d.totalCommandes ?? 0, cfa: false, type: 'commandes' },
      { icon: 'users', label: 'Total clients', valeur: d.totalClients ?? 0, cfa: false, type: 'clients' },
      { icon: 'x', label: 'Commandes impayées', valeur: d.commandesImpayees ?? 0, cfa: false, type: 'impaye' },
      { icon: 'x', label: 'Montant impayé', valeur: d.montantImpayeTotal ?? 0, cfa: true, type: 'impaye' },
    ];
  }

  commandesParStatut(): LigneStatut[] {
    return Object.entries(this.data()?.commandesParStatut ?? {}).map(([statut, count]) => ({
      cle: statut,
      statut,
      count,
      couleur: COULEUR_STATUT[statut] ?? 'var(--ink-soft)',
    }));
  }

  encaissementsParMoyen(): LigneMoyen[] {
    return Object.entries(this.data()?.encaissementParMoyen ?? {}).map(([moyen, montant]) => ({
      cle: moyen,
      moyen,
      montant,
      couleur: COULEUR_MOYEN[moyen] ?? 'var(--indigo)',
    }));
  }

  anneauStatuts(): SegmentAnneau[] {
    return construireAnneau(
      this.commandesParStatut().map((r) => ({
        cle: r.cle,
        legende: this.libelleStatut(r.statut),
        valeur: r.count,
        couleur: r.couleur,
      })),
    );
  }

  anneauMoyens(): SegmentAnneau[] {
    return construireAnneau(
      this.encaissementsParMoyen().map((r) => ({
        cle: r.cle,
        legende: this.libelleMoyen(r.moyen),
        valeur: r.montant,
        couleur: r.couleur,
      })),
    );
  }

  totalStatuts(): number {
    return this.commandesParStatut().reduce((s, r) => s + r.count, 0);
  }

  totalMoyens(): number {
    return this.encaissementsParMoyen().reduce((s, r) => s + r.montant, 0);
  }

  topClients(): LigneTop[] {
    const d = this.data();
    const liste = d?.topClients ?? [];
    const max = Math.max(1, ...liste.map((c) => c.totalDepense ?? 0));
    return liste
      .map((c, i) => ({
        cle: `${i}-${c.nom}`,
        rang: i + 1,
        rangClasse: i === 0 ? 'top-1' : i === 1 ? 'top-2' : i === 2 ? 'top-3' : '',
        nom: c.nom ?? '—',
        annexe: `${c.nbCommandes ?? 0} commande(s) · ${c.telephone ?? ''}`,
        total: c.totalDepense ?? 0,
        cfa: true,
        pct: Math.round(((c.totalDepense ?? 0) / max) * 100),
        couleur: 'var(--indigo)',
      }))
      .slice(0, 5);
  }

  topPrestations(): LigneTop[] {
    const d = this.data();
    const liste = d?.topPrestations ?? [];
    const max = Math.max(1, ...liste.map((p) => p.total ?? 0));
    return liste
      .map((p, i) => {
        const type = p.typeNettoyage ?? '';
        return {
          cle: `${i}-${p.nomTarif}`,
          rang: 0,
          rangClasse: '',
          nom: `${p.nomTarif ?? '—'}${p.typevetement ? ` (${p.typevetement})` : ''}`,
          annexe: `${p.nbPrestations ?? 0} prestation(s) · ${this.libelleTypeNettoyage(type)}`,
          total: p.total ?? 0,
          cfa: true,
          pct: Math.round(((p.total ?? 0) / max) * 100),
          couleur: COULEUR_NETTOYAGE[type] ?? 'var(--teal)',
        };
      })
      .slice(0, 5);
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

  libelleTypeNettoyage(t: string): string {
    return (LIBELLE_TYPE_NETTOYAGE as Record<string, string>)[t] ?? t;
  }
}
