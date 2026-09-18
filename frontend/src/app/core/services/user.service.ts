import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import { UserDTO, UserRequestDTO } from '../models/users.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private readonly http: HttpClient) {}

  creer(dto: UserRequestDTO): Observable<UserDTO> {
    return this.http.post<UserDTO>(`${API_URL}/api/users`, dto);
  }

  lister(): Observable<UserDTO[]> {
    return this.http.get<UserDTO[]>(`${API_URL}/api/users`);
  }

  listerLivreurs(): Observable<UserDTO[]> {
    return this.http.get<UserDTO[]>(`${API_URL}/api/users/livreurs`);
  }

  obtenir(id: number): Observable<UserDTO> {
    return this.http.get<UserDTO>(`${API_URL}/api/users/${id}`);
  }

  mettreAJour(id: number, dto: UserRequestDTO): Observable<UserDTO> {
    return this.http.put<UserDTO>(`${API_URL}/api/users/${id}`, dto);
  }

  changerStatut(id: number, actif: boolean): Observable<UserDTO> {
    return this.http.patch<UserDTO>(`${API_URL}/api/users/${id}/statut`, { actif });
  }

  changerMotDePasse(id: number, password: string): Observable<UserDTO> {
    return this.http.post<UserDTO>(`${API_URL}/api/users/${id}/mot-de-passe`, { password });
  }
}