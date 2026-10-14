import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import { SuiviDTO } from '../models/suivi.model';

@Injectable({ providedIn: 'root' })
export class SuiviService {
  constructor(private readonly http: HttpClient) {}

  suivre(numeroTicket: string): Observable<SuiviDTO> {
    return this.http.get<SuiviDTO>(`${API_URL}/api/suivi/${encodeURIComponent(numeroTicket.trim())}`);
  }
}
