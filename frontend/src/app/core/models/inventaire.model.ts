export interface VetementDTO {
  idvetement?: number;
  libelle?: string;
  typedevetement?: string;
  quantiteStock?: number;
}

export interface LigneInventaireDTO {
  idligneinventaire?: number;
  vetementId?: number;
  vetementLibelle?: string;
  quantite?: number;
  dateEntree?: string;
  etat?: string;
}

export interface BonInventaireDTO {
  idinventaire?: number;
  dateInventaire?: string;
  observations?: string;
  totalQuantite?: number;
  lignes?: LigneInventaireDTO[];
}

export interface LigneBonInventaireRequestDTO {
  vetementId: number;
  quantite: number;
  etat?: string;
}

export interface BonInventaireRequestDTO {
  date?: string;
  observations?: string;
  lignes: LigneBonInventaireRequestDTO[];
}