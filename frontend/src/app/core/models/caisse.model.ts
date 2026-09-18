export interface DetailParMoyen {
  moyen?: string;
  montant?: number;
  nombre?: number;
}

export interface ClotureDTO {
  idcloture?: number;
  date?: string;
  dateCloture?: string;
  totalLogiciel?: number;
  totalCompte?: number;
  ecart?: number;
  nombrePaiements?: number;
  detailParMoyen?: string;
  observations?: string;
  cloturee?: boolean;
  parMoyen?: DetailParMoyen[];
}

export interface ClotureRequestDTO {
  date?: string;
  montantEnCaisse: number;
  observations?: string;
}