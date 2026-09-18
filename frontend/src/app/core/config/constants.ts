export const API_URL = '';

export const ROLES = {
  ADMIN: 'ADMIN',
  RECEPTIONNISTE: 'RECEPTIONNISTE',
  AGENT_PRODUCTION: 'AGENT_PRODUCTION',
  LIVREUR: 'LIVREUR',
} as const;

export type Role = (typeof ROLES)[keyof typeof ROLES];

export type StatutCommande = (typeof STATUT_COMMANDE)[keyof typeof STATUT_COMMANDE];
export type StatutLivraison = (typeof STATUT_LIVRAISON)[keyof typeof STATUT_LIVRAISON];
export type TypeNettoyage = (typeof TYPE_NETTOYAGE)[keyof typeof TYPE_NETTOYAGE];
export type MoyenPaiement = (typeof MOYEN_PAIEMENT)[keyof typeof MOYEN_PAIEMENT];
export type TypePaiement = (typeof TYPE_PAIEMENT)[keyof typeof TYPE_PAIEMENT];

export const STATUT_COMMANDE = {
  RECU: 'RECU',
  INVENTAIRE: 'INVENTAIRE',
  EN_LAVAGE: 'EN_LAVAGE',
  REPASSAGE: 'REPASSAGE',
  PRET: 'PRET',
  RECUPERE: 'RECUPERE',
  LIVRE: 'LIVRE',
  PAYEE: 'PAYEE',
} as const;

export const STATUT_LIVRAISON = {
  A_LIVRER: 'A_LIVRER',
  EN_COURS: 'EN_COURS',
  LIVREE: 'LIVREE',
  ECHEC: 'ECHEC',
} as const;

export const TYPE_NETTOYAGE = {
  LAVAGE_SIMPLE: 'LAVAGE_SIMPLE',
  NETTOYAGE_A_SEC: 'NETTOYAGE_A_SEC',
  REPASSAGE: 'REPASSAGE',
  LAUNDRY: 'LAUNDRY',
} as const;

export const MOYEN_PAIEMENT = {
  ESPECES: 'ESPECES',
  ORANGE_MONEY: 'ORANGE_MONEY',
  MTN_MONEY: 'MTN_MONEY',
} as const;

export const TYPE_PAIEMENT = {
  ACOMPTE: 'ACOMPTE',
  SOLDE: 'SOLDE',
  INTEGRAL: 'INTEGRAL',
} as const;