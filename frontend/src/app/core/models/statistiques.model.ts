export interface TopClientDTO {
  nom?: string;
  telephone?: string;
  nbCommandes?: number;
  totalDepense?: number;
}

export interface TopPrestationDTO {
  nomTarif?: string;
  typevetement?: string;
  typeNettoyage?: string;
  nbPrestations?: number;
  total?: number;
}

export interface StatistiquesDTO {
  chiffreAffairesJour?: number;
  chiffreAffairesMois?: number;
  totalEncaisse?: number;
  totalCommandes?: number;
  commandesAujourdhui?: number;
  totalClients?: number;
  commandesImpayees?: number;
  montantImpayeTotal?: number;
  commandesParStatut?: Record<string, number>;
  encaissementParMoyen?: Record<string, number>;
  topClients?: TopClientDTO[];
  topPrestations?: TopPrestationDTO[];
}