import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import { NotificationDTO } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  constructor(private readonly http: HttpClient) {}

  mesNotifications(): Observable<NotificationDTO[]> {
    return this.http.get<NotificationDTO[]>(`${API_URL}/api/notifications`);
  }

  compterNonLues(): Observable<number> {
    return this.http.get<number>(`${API_URL}/api/notifications/non-lues`);
  }

  marquerLue(id: number): Observable<void> {
    return this.http.patch<void>(`${API_URL}/api/notifications/${id}/lue`, {});
  }

  toutLue(): Observable<void> {
    return this.http.patch<void>(`${API_URL}/api/notifications/tout-lue`, {});
  }
}