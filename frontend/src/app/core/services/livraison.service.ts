import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import { LivraisonDTO, LivraisonRequestDTO } from '../models/livraison.model';

@Injectable({ providedIn: 'root' })
export class LivraisonService {
  constructor(private readonly http: HttpClient) {}

  lister(statut?: string): Observable<LivraisonDTO[]> {
    let params = new HttpParams();
    if (statut) params = params.set('statut', statut);
    return this.http.get<LivraisonDTO[]>(`${API_URL}/api/livraisons`, { params });
  }

  mesLivraisons(statut?: string): Observable<LivraisonDTO[]> {
    let params = new HttpParams();
    if (statut) params = params.set('statut', statut);
    return this.http.get<LivraisonDTO[]>(`${API_URL}/api/livraisons/mes-livraisons`, { params });
  }

  assigner(id: number, dto: LivraisonRequestDTO): Observable<LivraisonDTO> {
    return this.http.post<LivraisonDTO>(`${API_URL}/api/livraisons/${id}/assigner`, dto);
  }

  changerStatut(id: number, statut: string): Observable<LivraisonDTO> {
    return this.http.patch<LivraisonDTO>(`${API_URL}/api/livraisons/${id}/statut`, { statut });
  }

  masquer(id: number): Observable<LivraisonDTO> {
    return this.http.patch<LivraisonDTO>(`${API_URL}/api/livraisons/${id}/masquer`, {});
  }
}