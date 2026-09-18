export interface PaiementDTO {
  montant: number;
  moyenPaiement: string;
  referenceTransaction?: string;
}

export interface PaiementResponseDTO {
  idpaiement?: number;
  montant?: number;
  moyenPaiement?: string;
  typePaiement?: string;
  referenceTransaction?: string;
  datePaiement?: string;
  idcommande?: number;
  numeroTicket?: string;
}