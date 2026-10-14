import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { DateFrPipe } from '../../shared/pipes/date.fr.pipe';
import { Icon } from '../../shared/icon/icon';
import { LIBELLE_STATUT_COMMANDE } from '../../shared/libelles';
import { SuiviService } from '../../core/services/suivi.service';
import { SuiviDTO, LigneSuivi } from '../../core/models/suivi.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';
import { API_URL } from '../../core/config/constants';

interface Etape {
  cle: string;
  label: string;
  desc: string;
}

@Component({
  selector: 'app-suivi',
  imports: [FormsModule, CfaPipe, DateFrPipe, Icon],
  templateUrl: './suivi.html',
  styleUrl: './suivi.scss',
})
export class Suivi {
  private readonly service = inject(SuiviService);

  readonly recherche = signal('');
  readonly resultat = signal<SuiviDTO | null>(null);
  readonly enChargement = signal(false);
  readonly rechercheEffectuee = signal(false);
  readonly erreur = signal('');

  readonly etapes: Etape[] = [
    { cle: 'RECU', label: '1. Dépôt', desc: 'Enregistrement & étiquetage' },
    { cle: 'EN_LAVAGE', label: '2. Lavage', desc: 'Traitement détachant & bain' },
    { cle: 'REPASSAGE', label: '3. Repassage', desc: 'Finition vapeur & amidon' },
    { cle: 'PRET', label: '4. Prêt au retrait', desc: 'En casier sous housse' },
    { cle: 'CLOTURE', label: '5. Livré', desc: 'Remis & clôturé' },
  ];

  statutIndex(statut: string | undefined): number {
    switch (statut) {
      case 'RECU':
      case 'INVENTAIRE':
        return 0;
      case 'EN_LAVAGE':
        return 1;
      case 'REPASSAGE':
        return 2;
      case 'PRET':
        return 3;
      case 'RECUPERE':
      case 'LIVRE':
      case 'PAYEE':
        return 4;
      default:
        return 0;
    }
  }

  libelleStatut(statut: string | undefined): string {
    return (LIBELLE_STATUT_COMMANDE as Record<string, string>)[statut ?? ''] ?? (statut ?? '');
  }

  estPret(): boolean {
    return this.resultat()?.statut === 'PRET';
  }

  totalPieces(): number {
    return (this.resultat()?.lignes ?? [])
      .filter((l) => this.estPiece(l))
      .reduce((s, l) => s + (l.quantite ?? 0), 0);
  }

  totalKilos(): number {
    return (this.resultat()?.lignes ?? [])
      .filter((l) => !this.estPiece(l))
      .reduce((s, l) => s + (l.poids ?? 0), 0);
  }

  estPiece(l: LigneSuivi): boolean {
    return (l.unite ?? 'PIECE') !== 'KILO';
  }

  rechercher(): void {
    const code = this.recherche().trim().replace('#', '');
    if (!code) {
      this.erreur.set('Saisissez le numéro de ticket figurant sur votre reçu.');
      return;
    }
    this.enChargement.set(true);
    this.erreur.set('');
    this.resultat.set(null);
    this.rechercheEffectuee.set(true);
    this.service.suivre(code).subscribe({
      next: (r) => {
        this.resultat.set(r);
        this.enChargement.set(false);
      },
      error: (err) => {
        this.enChargement.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  ouvrirRecu(): void {
    const ticket = this.resultat()?.numeroTicket;
    if (ticket) {
      window.open(`${API_URL}/recu/${ticket}`, '_blank');
    }
  }
}
