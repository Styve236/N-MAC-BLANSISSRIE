import { Component, input } from '@angular/core';

const CLASSES: Record<string, string> = {
  RECU: 'pill-blue',
  INVENTAIRE: 'pill-amber',
  EN_LAVAGE: 'pill-amber',
  REPASSAGE: 'pill-amber',
  PRET: 'pill-green',
  RECUPERE: 'pill-green',
  LIVRE: 'pill-green',
  PAYEE: 'pill-green',
  A_LIVRER: 'pill-blue',
  EN_COURS: 'pill-amber',
  LIVREE: 'pill-green',
  ECHEC: 'pill-red',
};

const LIBELLES: Record<string, string> = {
  RECU: 'Reçu',
  INVENTAIRE: 'Inventaire',
  EN_LAVAGE: 'En lavage',
  REPASSAGE: 'Repassage',
  PRET: 'Prêt',
  RECUPERE: 'Récupéré',
  LIVRE: 'Livré',
  PAYEE: 'Payée',
  A_LIVRER: 'À livrer',
  EN_COURS: 'En cours',
  LIVREE: 'Livrée',
  ECHEC: 'Échec',
};

@Component({
  selector: 'app-statut-badge',
  imports: [],
  template: `<span class="pill" [class]="classe()">{{ libelle() }}</span>`,
  styles: `
    .pill {
      display: inline-block;
      padding: 3px 9px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.4px;
      white-space: nowrap;
    }
    .pill-blue { background: var(--primaire-soft); color: var(--primaire); }
    .pill-amber { background: var(--avertissement-soft); color: var(--avertissement); }
    .pill-green { background: var(--succes-soft); color: var(--succes); }
    .pill-red { background: var(--danger-soft); color: var(--danger); }
  `,
})
export class StatutBadge {
  readonly statut = input<string>('');

  protected classe(): string {
    return CLASSES[this.statut()] ?? 'pill-blue';
  }

  protected libelle(): string {
    return LIBELLES[this.statut()] ?? this.statut();
  }
}