export interface LigneSuivi {
  designation?: string;
  typeNettoyage?: string;
  unite?: string;
  quantite?: number;
  poids?: number;
  montant?: number;
}

export interface SuiviDTO {
  numeroTicket?: string;
  statut?: string;
  nomClient?: string;
  telephoneMasque?: string;
  dateCreation?: string;
  dateRecuperationPrevue?: string;
  poidsTotal?: number;
  montantTotal?: number;
  montantPaye?: number;
  resteAPayer?: number;
  lignes?: LigneSuivi[];
}
