import type { MoyenPaiement, StatutCommande, StatutLivraison, TypeNettoyage } from '../core/config/constants';

export const LIBELLE_TYPE_NETTOYAGE: Record<TypeNettoyage, string> = {
  LAVAGE_SIMPLE: 'Lavage simple',
  NETTOYAGE_A_SEC: 'Nettoyage à sec',
  REPASSAGE: 'Repassage',
  LAUNDRY: 'Laundry',
};

export const LIBELLE_STATUT_COMMANDE: Record<StatutCommande, string> = {
  RECU: 'Reçu',
  INVENTAIRE: 'Inventaire',
  EN_LAVAGE: 'En lavage',
  REPASSAGE: 'Repassage',
  PRET: 'Prêt',
  RECUPERE: 'Récupéré',
  LIVRE: 'Livré',
  PAYEE: 'Payée',
};

export const LIBELLE_STATUT_LIVRAISON: Record<StatutLivraison, string> = {
  A_LIVRER: 'À livrer',
  EN_COURS: 'En cours',
  LIVREE: 'Livrée',
  ECHEC: 'Échec',
};

export const LIBELLE_MOYEN_PAIEMENT: Record<MoyenPaiement, string> = {
  ESPECES: 'Espèces',
  ORANGE_MONEY: 'Orange Money',
  MTN_MONEY: 'MTN Money',
};

export function typeNettoyageOptions(): { value: string; label: string }[] {
  return Object.entries(LIBELLE_TYPE_NETTOYAGE).map(([value, label]) => ({ value, label }));
}

export function moyenPaiementOptions(): { value: string; label: string }[] {
  return Object.entries(LIBELLE_MOYEN_PAIEMENT).map(([value, label]) => ({ value, label }));
}