export interface LivraisonDTO {
  idcommande?: number;
  numeroTicket?: string;
  clientNom?: string;
  clientTelephone?: string;
  adresseLivraison?: string;
  fraisLivraison?: number;
  dateLivraisonPrevue?: string;
  dateLivraisonReelle?: string;
  livreurId?: number;
  livreurNom?: string;
  statutLivraison?: string;
  montantTotal?: number;
  montantPaye?: number;
  payee?: boolean;
}

export interface LivraisonRequestDTO {
  adresseLivraison?: string;
  livreurId?: number;
  dateLivraisonPrevue?: string;
  fraisLivraison?: number;
  statut?: string;
}