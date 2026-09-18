import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import { ClotureDTO, ClotureRequestDTO } from '../models/caisse.model';

@Injectable({ providedIn: 'root' })
export class CaisseService {
  constructor(private readonly http: HttpClient) {}

  rapport(date?: string): Observable<ClotureDTO> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.get<ClotureDTO>(`${API_URL}/api/caisse/rapport`, { params });
  }

  cloturer(dto: ClotureRequestDTO): Observable<ClotureDTO> {
    return this.http.post<ClotureDTO>(`${API_URL}/api/caisse/cloturer`, dto);
  }

  historique(): Observable<ClotureDTO[]> {
    return this.http.get<ClotureDTO[]>(`${API_URL}/api/caisse/clotures`);
  }

  ticket(date?: string): Observable<Blob> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.get(`${API_URL}/api/caisse/ticket`, { params, responseType: 'blob' });
  }
}