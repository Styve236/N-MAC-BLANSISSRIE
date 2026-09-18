import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import { DashboardResumeDTO } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private readonly http: HttpClient) {}

  resume(): Observable<DashboardResumeDTO> {
    return this.http.get<DashboardResumeDTO>(`${API_URL}/api/dashboard/resume`);
  }
}