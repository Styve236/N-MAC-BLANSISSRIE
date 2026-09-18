import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import { Tarif } from '../models/commande.model';

@Injectable({ providedIn: 'root' })
export class TarifService {
  constructor(private readonly http: HttpClient) {}

  creer(tarif: Tarif): Observable<Tarif> {
    return this.http.post<Tarif>(`${API_URL}/api/tarifs`, tarif);
  }

  lister(typeNettoyage?: string, page = 0, size = 20): Observable<any> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (typeNettoyage) params = params.set('typeNettoyage', typeNettoyage);
    return this.http.get(`${API_URL}/api/tarifs`, { params });
  }

  actifs(): Observable<Tarif[]> {
    return this.http.get<Tarif[]>(`${API_URL}/api/tarifs/actifs`);
  }

  obtenir(id: number): Observable<Tarif> {
    return this.http.get<Tarif>(`${API_URL}/api/tarifs/${id}`);
  }

  mettreAJour(id: number, tarif: Tarif): Observable<Tarif> {
    return this.http.put<Tarif>(`${API_URL}/api/tarifs/${id}`, tarif);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/api/tarifs/${id}`);
  }

  activer(id: number): Observable<Tarif> {
    return this.http.patch<Tarif>(`${API_URL}/api/tarifs/${id}/activer`, {});
  }
}