import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import { PaiementDTO, PaiementResponseDTO } from '../models/paiement.model';
import { StatutPaiementDTO } from '../models/commande.model';

@Injectable({ providedIn: 'root' })
export class PaiementService {
  constructor(private readonly http: HttpClient) {}

  caDuJour(): Observable<number> {
    return this.http.get<number>(`${API_URL}/api/paiements/ca-du-jour`);
  }

  lister(clientId?: number, page = 0, size = 20): Observable<any> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (clientId) params = params.set('clientId', clientId);
    return this.http.get(`${API_URL}/api/paiements`, { params });
  }

  payer(commandeId: number, dto: PaiementDTO): Observable<PaiementResponseDTO> {
    return this.http.post<PaiementResponseDTO>(`${API_URL}/api/commandes/${commandeId}/payer`, dto);
  }

  resteAPayer(commandeId: number): Observable<number> {
    return this.http.get<number>(`${API_URL}/api/commandes/${commandeId}/payer`);
  }

  parCommande(commandeId: number): Observable<PaiementResponseDTO[]> {
    return this.http.get<PaiementResponseDTO[]>(`${API_URL}/api/commandes/${commandeId}/paiements`);
  }

  statutPaiement(commandeId: number): Observable<StatutPaiementDTO> {
    return this.http.get<StatutPaiementDTO>(`${API_URL}/api/commandes/${commandeId}/statut-paiement`);
  }

  parClient(clientId: number): Observable<PaiementResponseDTO[]> {
    return this.http.get<PaiementResponseDTO[]>(`${API_URL}/api/clients/${clientId}/paiements`);
  }
}