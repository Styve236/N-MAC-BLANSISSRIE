import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import {
  CrediterPointsRequestDTO,
  HistoriquePoint,
  SoldeFideliteDTO,
  UtiliserPointsRequestDTO,
  UtiliserPointsResponseDTO,
} from '../models/fidelite.model';

@Injectable({ providedIn: 'root' })
export class FideliteService {
  constructor(private readonly http: HttpClient) {}

  solde(clientId: number): Observable<SoldeFideliteDTO> {
    return this.http.get<SoldeFideliteDTO>(`${API_URL}/api/fidelite/solde/${clientId}`);
  }

  historique(clientId: number): Observable<HistoriquePoint[]> {
    return this.http.get<HistoriquePoint[]>(`${API_URL}/api/fidelite/historique/${clientId}`);
  }

  utiliser(dto: UtiliserPointsRequestDTO): Observable<UtiliserPointsResponseDTO> {
    return this.http.post<UtiliserPointsResponseDTO>(`${API_URL}/api/fidelite/utiliser`, dto);
  }

  crediter(dto: CrediterPointsRequestDTO): Observable<void> {
    return this.http.post<void>(`${API_URL}/api/fidelite/crediter`, dto);
  }
}