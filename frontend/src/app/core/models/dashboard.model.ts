export interface TendanceDTO {
  actuel?: number;
  precedent?: number;
  variation?: number;
}

export interface DashboardTendancesDTO {
  caJour?: TendanceDTO;
  caSemaine?: TendanceDTO;
  caMois?: TendanceDTO;
  commandesEnCours?: number;
  commandesTerminees?: number;
  tauxTransformation?: number;
}

export interface AlerteCommandeRetardDTO {
  commandeId?: number;
  numeroTicket?: string;
  clientNom?: string;
  clientTelephone?: string;
  dateRecuperationPrevue?: string;
  joursRetard?: number;
}

export interface AlerteClientImpayeDTO {
  clientId?: number;
  nom?: string;
  telephone?: string;
  montantImpaye?: number;
  nbCommandesImpayees?: number;
}

export interface AlerteStockCritiqueDTO {
  vetementId?: number;
  libelle?: string;
  typedevetement?: string;
  quantiteStock?: number;
  seuil?: number;
}

export interface DashboardResumeDTO {
  tendances?: DashboardTendancesDTO;
  commandesEnRetard: AlerteCommandeRetardDTO[];
  clientsImpayes: AlerteClientImpayeDTO[];
  stockCritique: AlerteStockCritiqueDTO[];
}