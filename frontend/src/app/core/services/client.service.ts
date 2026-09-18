import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import { Client } from '../models/client.model';

@Injectable({ providedIn: 'root' })
export class ClientService {
  constructor(private readonly http: HttpClient) {}

  creer(client: Client): Observable<Client> {
    return this.http.post<Client>(`${API_URL}/api/clients`, client);
  }

  lister(page = 0, size = 20, sort = 'nom'): Observable<any> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', sort);
    return this.http.get(`${API_URL}/api/clients`, { params });
  }

  obtenir(id: number): Observable<Client> {
    return this.http.get<Client>(`${API_URL}/api/clients/${id}`);
  }

  rechercherParTelephone(telephone: string): Observable<Client> {
    return this.http.get<Client>(`${API_URL}/api/clients/recherche/telephone`, {
      params: new HttpParams().set('telephone', telephone),
    });
  }

  rechercherParNom(nom: string): Observable<any> {
    return this.http.get(`${API_URL}/api/clients/recherche/nom`, {
      params: new HttpParams().set('nom', nom),
    });
  }
}