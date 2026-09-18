import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';
import { SmsNotificationDTO } from '../models/sms.model';

@Injectable({ providedIn: 'root' })
export class SmsService {
  constructor(private readonly http: HttpClient) {}

  lister(): Observable<SmsNotificationDTO[]> {
    return this.http.get<SmsNotificationDTO[]>(`${API_URL}/api/sms`);
  }
}