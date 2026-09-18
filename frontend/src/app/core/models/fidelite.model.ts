export interface SoldeFideliteDTO {
  clientId?: number;
  points?: number;
  valeurEstimee?: string;
}

export interface HistoriquePoint {
  idpoint?: number;
  clientId?: number;
  commandeId?: number;
  points?: number;
  type?: string;
  description?: string;
  date?: string;
}

export interface UtiliserPointsRequestDTO {
  clientId: number;
  commandeId: number;
  nbPoints: number;
}

export interface UtiliserPointsResponseDTO {
  remise?: number;
  nbPointsUtilises?: number;
  message?: string;
}

export interface CrediterPointsRequestDTO {
  clientId: number;
  nbPoints: number;
  description?: string;
}