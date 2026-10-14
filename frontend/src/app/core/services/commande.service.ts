import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import { Commande, CommandeDTO, CommandeRequestDTO, RecuDTO } from '../models/commande.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class CommandeService {
  constructor(private readonly http: HttpClient) {}

  creer(dto: CommandeRequestDTO): Observable<Commande> {
    return this.http.post<Commande>(`${API_URL}/api/commandes`, dto);
  }

  lister(filtres?: {
    clientId?: number;
    statut?: string;
    dateDebut?: string;
    dateFin?: string;
    archivee?: boolean;
    page?: number;
    size?: number;
    sort?: string;
  }): Observable<Page<CommandeDTO>> {
    let params = new HttpParams();
    if (filtres) {
      if (filtres.clientId) params = params.set('clientId', filtres.clientId);
      if (filtres.statut) params = params.set('statut', filtres.statut);
      if (filtres.dateDebut) params = params.set('dateDebut', filtres.dateDebut);
      if (filtres.dateFin) params = params.set('dateFin', filtres.dateFin);
      if (filtres.archivee != null) params = params.set('archivee', filtres.archivee);
      if (filtres.page != null) params = params.set('page', filtres.page);
      if (filtres.size != null) params = params.set('size', filtres.size);
      if (filtres.sort) params = params.set('sort', filtres.sort);
    }
    return this.http.get<Page<CommandeDTO>>(`${API_URL}/api/commandes`, { params });
  }

  archiver(id: number): Observable<Commande> {
    return this.http.patch<Commande>(`${API_URL}/api/commandes/${id}/archiver`, {});
  }

  restaurer(id: number): Observable<Commande> {
    return this.http.patch<Commande>(`${API_URL}/api/commandes/${id}/restaurer`, {});
  }

  obtenir(id: number): Observable<Commande> {
    return this.http.get<Commande>(`${API_URL}/api/commandes/${id}`);
  }

  changerStatut(id: number, statut: string): Observable<Commande> {
    return this.http.patch<Commande>(`${API_URL}/api/commandes/${id}/statut`, { statut });
  }

  parClient(clientId: number): Observable<CommandeDTO[]> {
    return this.http.get<CommandeDTO[]>(`${API_URL}/api/commandes/client/${clientId}`);
  }

  marquerPret(id: number): Observable<Commande> {
    return this.http.post<Commande>(`${API_URL}/api/commandes/${id}/pret`, {});
  }

  rappelImpayes(): Observable<number> {
    return this.http.post<number>(`${API_URL}/api/commandes/rappel-impayes`, {});
  }

  recu(id: number): Observable<RecuDTO> {
    return this.http.get<RecuDTO>(`${API_URL}/api/commandes/${id}/recu`);
  }

  envoyerRecu(id: number): Observable<RecuDTO> {
    return this.http.post<RecuDTO>(`${API_URL}/api/commandes/${id}/recu/envoyer`, {});
  }

  recuPdfUrl(id: number): string {
    return `${API_URL}/api/commandes/${id}/recu/pdf`;
  }
}