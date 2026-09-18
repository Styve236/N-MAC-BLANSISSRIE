import type { TypeNettoyage } from '../config/constants';

export interface Tarif {
  idtarif?: number;
  typevetement?: string;
  prixauklo?: number;
  prixunitaire?: number;
  nom?: string;
  typeNettoyage?: TypeNettoyage;
  actif?: boolean;
}

export interface Photo {
  idphoto?: number;
  ligneId?: number;
  url?: string;
  typephoto?: string;
  dateTime?: string;
}

export interface LigneCommandeLegere {
  idligne?: number;
  poids?: number;
  quantite?: number;
  montant?: number;
  description?: string;
  typeNettoyage?: TypeNettoyage;
  tarif: Tarif;
}

export interface LigneCommandeDTO {
  tarifId: number;
  poids?: number;
  quantite?: number;
  typeNettoyage?: TypeNettoyage;
  description?: string;
}

export interface PaiementResume {
  idpaiement?: number;
  montant?: number;
  moyenPaiement?: string;
  typePaiement?: string;
  referenceTransaction?: string;
  datePaiement?: string;
}

export interface Commande {
  idcommande?: number;
  numeroTicket?: string;
  client?: { idclient?: number; nom: string; telephone: string; ville?: string; quartier?: string };
  dateCreation?: string;
  dateRecuperationPrevue?: string;
  dateRetraitReelle?: string;
  statut?: string;
  poidsTotal?: number;
  montantTotal?: number;
  montantPaye?: number;
  remise?: number;
  adresseLivraison?: string;
  fraisLivraison?: number;
  statutLivraison?: string;
  lignes?: LigneCommandeLegere[];
  paiements?: PaiementResume[];
}

export interface CommandeRequestDTO {
  clientId: number;
  lignes: LigneCommandeDTO[];
  dateRetraitPrevue?: string;
  acompte?: number;
  moyenAcompte?: string;
  messageAgent?: string;
}

export interface CommandeDTO {
  idcommande?: number;
  numeroTicket?: string;
  dateCreation?: string;
  dateRecuperationPrevue?: string;
  statut?: string;
  poidsTotal?: number;
  montantTotal?: number;
  montantPaye?: number;
  remise?: number;
  resteAPayer?: number;
  payee?: boolean;
}

export interface StatutPaiementDTO {
  idcommande?: number;
  numeroTicket?: string;
  montantTotal?: number;
  montantPaye?: number;
  resteAPayer?: number;
  payee?: boolean;
  paiements?: PaiementResume[];
}

export interface RecuDTO {
  idcommande?: number;
  numeroTicket?: string;
  lien?: string;
  statut?: string;
  nomClient?: string;
  telephoneClient?: string;
  dateCreation?: string;
  dateRecuperationPrevue?: string;
  poidsTotal?: number;
  montantTotal?: number;
  remise?: number;
  montantPaye?: number;
  resteAPayer?: number;
  lignes?: LigneCommandeLegere[];
  paiements?: PaiementResume[];
}