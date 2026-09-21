import { Component, inject, signal } from '@angular/core';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { PercentPipe } from '@angular/common';
import { Icon } from '../../shared/icon/icon';
import { LIBELLE_STATUT_COMMANDE, LIBELLE_MOYEN_PAIEMENT } from '../../shared/libelles';
import { DashboardResumeDTO } from '../../core/models/dashboard.model';
import { DashboardService } from '../../core/services/dashboard.service';
import {
  SegmentAnneau,
  construireAnneau,
  CIRCONFERENCE,
  COULEUR_STATUT,
  COULEUR_MOYEN,
} from '../../shared/lib/anneau.util';

interface Kpi {
  icon: string;
  label: string;
  valeur: number;
  cfa: boolean;
  type: 'revenu' | 'commandes' | 'impaye';
}

interface CarteAlerte {
  cle: string;
  titre: string;
  icone: string;
  lignes: { libelle: string; detail: string; valeur: string; style: string }[];
}

@Component({
  selector: 'app-dashboard',
  imports: [CfaPipe, PercentPipe, Icon],
  templateUrl: './dashboard.html',
  styleUrl: '../_feature.scss',
})
export class Dashboard {
  private readonly service = inject(DashboardService);

  readonly data = signal<DashboardResumeDTO | null>(null);
  readonly enChargement = signal(true);
  readonly anime = signal(false);

  constructor() {
    this.service.resume().subscribe({
      next: (r) => {
        this.data.set(r);
        this.enChargement.set(false);
        setTimeout(() => this.anime.set(true), 60);
      },
      error: () => this.enChargement.set(false),
    });
  }

  dasharraySegment(seg: SegmentAnneau): string {
    return this.anime() ? seg.dasharray : `0 ${CIRCONFERENCE}`;
  }

  kpis(): Kpi[] {
    const d = this.data();
    if (!d) return [];
    const t = d.tendances;
    const impaye = (d.clientsImpayes ?? []).reduce((s, c) => s + (c.montantImpaye ?? 0), 0);
    return [
      { icon: 'caisse', label: 'CA du jour', valeur: t?.caJour?.actuel ?? 0, cfa: true, type: 'revenu' },
      { icon: 'caisse', label: 'CA de la semaine', valeur: t?.caSemaine?.actuel ?? 0, cfa: true, type: 'revenu' },
      { icon: 'caisse', label: 'CA du mois', valeur: t?.caMois?.actuel ?? 0, cfa: true, type: 'revenu' },
      { icon: 'commandes', label: 'Commandes en cours', valeur: t?.commandesEnCours ?? 0, cfa: false, type: 'commandes' },
      { icon: 'commandes', label: 'Commandes terminées', valeur: t?.commandesTerminees ?? 0, cfa: false, type: 'commandes' },
      { icon: 'rapports', label: 'Taux de transformation', valeur: t?.tauxTransformation ?? 0, cfa: false, type: 'commandes' },
      { icon: 'x', label: 'Commandes impayées', valeur: (d.clientsImpayes ?? []).length, cfa: false, type: 'impaye' },
      { icon: 'x', label: 'Montant impayé', valeur: impaye, cfa: true, type: 'impaye' },
    ];
  }

  commandesParStatut(): { cle: string; statut: string; count: number; couleur: string }[] {
    const m = this.data()?.commandesParStatut ?? {};
    return Object.entries(m).map(([statut, count]) => ({
      cle: statut,
      statut,
      count,
      couleur: COULEUR_STATUT[statut] ?? 'var(--ink-soft)',
    }));
  }

  encaissementsParMoyen(): { cle: string; moyen: string; montant: number; couleur: string }[] {
    const m = this.data()?.encaissementParMoyen ?? {};
    return Object.entries(m).map(([moyen, montant]) => ({
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

  cartesAlerte(): CarteAlerte[] {
    const d = this.data();
    if (!d) return [];
    return [
      {
        cle: 'retard',
        titre: 'Commandes en retard',
        icone: 'bell',
        lignes: (d.commandesEnRetard ?? []).map((c) => ({
          libelle: c.numeroTicket ?? '',
          detail: c.clientNom ?? '',
          valeur: `${c.joursRetard ?? 0} j`,
          style: 'corail',
        })),
      },
      {
        cle: 'impayes',
        titre: 'Clients impayés',
        icone: 'users',
        lignes: (d.clientsImpayes ?? []).map((c) => ({
          libelle: c.nom ?? '',
          detail: c.telephone ?? '',
          valeur: (c.montantImpaye ?? 0).toLocaleString('fr-FR'),
          style: 'corail',
        })),
      },
      {
        cle: 'stock',
        titre: 'Stock critique',
        icone: 'inventaire',
        lignes: (d.stockCritique ?? []).map((s) => ({
          libelle: s.libelle ?? '',
          detail: s.typedevetement ?? '',
          valeur: `${s.quantiteStock ?? 0} restant(s)`,
          style: 'ambre',
        })),
      },
    ];
  }

  libelleStatut(s: string): string {
    return (LIBELLE_STATUT_COMMANDE as Record<string, string>)[s] ?? s;
  }

  libelleMoyen(m: string): string {
    return (LIBELLE_MOYEN_PAIEMENT as Record<string, string>)[m] ?? m;
  }
}