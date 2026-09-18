import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import { BonInventaireDTO, BonInventaireRequestDTO, VetementDTO } from '../models/inventaire.model';

@Injectable({ providedIn: 'root' })
export class InventaireService {
  constructor(private readonly http: HttpClient) {}

  vetements(): Observable<VetementDTO[]> {
    return this.http.get<VetementDTO[]>(`${API_URL}/api/inventaire/vetements`);
  }

  creerVetement(dto: VetementDTO): Observable<VetementDTO> {
    return this.http.post<VetementDTO>(`${API_URL}/api/inventaire/vetements`, dto);
  }

  mettreAJourVetement(id: number, dto: VetementDTO): Observable<VetementDTO> {
    return this.http.put<VetementDTO>(`${API_URL}/api/inventaire/vetements/${id}`, dto);
  }

  supprimerVetement(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/api/inventaire/vetements/${id}`);
  }

  bons(): Observable<BonInventaireDTO[]> {
    return this.http.get<BonInventaireDTO[]>(`${API_URL}/api/inventaire/bons`);
  }

  bon(id: number): Observable<BonInventaireDTO> {
    return this.http.get<BonInventaireDTO>(`${API_URL}/api/inventaire/bons/${id}`);
  }

  creerBon(dto: BonInventaireRequestDTO): Observable<BonInventaireDTO> {
    return this.http.post<BonInventaireDTO>(`${API_URL}/api/inventaire/bons`, dto);
  }
}