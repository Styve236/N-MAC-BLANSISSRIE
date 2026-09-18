import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import { StatistiquesDTO } from '../models/statistiques.model';

@Injectable({ providedIn: 'root' })
export class StatistiquesService {
  constructor(private readonly http: HttpClient) {}

  synthese(): Observable<StatistiquesDTO> {
    return this.http.get<StatistiquesDTO>(`${API_URL}/api/statistiques`);
  }
}