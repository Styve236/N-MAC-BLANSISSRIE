import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../config/constants';

export type FormatExport = 'pdf' | 'csv';
export type ExportType = 'commandes' | 'paiements' | 'caisse' | 'inventaire' | 'fidelite';

@Injectable({ providedIn: 'root' })
export class ExportService {
  constructor(private readonly http: HttpClient) {}

  commandes(debut?: string, fin?: string, format: FormatExport = 'pdf'): Observable<Blob> {
    return this.blob(`/api/exports/commandes${format === 'csv' ? '/csv' : ''}`, debut, fin);
  }

  paiements(debut: string, fin: string, format: FormatExport = 'pdf'): Observable<Blob> {
    return this.blob(`/api/exports/paiements${format === 'csv' ? '/csv' : ''}`, debut, fin);
  }

  caisse(format: FormatExport = 'pdf'): Observable<Blob> {
    return this.blob(`/api/exports/caisse${format === 'csv' ? '/csv' : ''}`);
  }

  inventaire(format: FormatExport = 'pdf'): Observable<Blob> {
    return this.blob(`/api/exports/inventaire${format === 'csv' ? '/csv' : ''}`);
  }

  fidelite(format: FormatExport = 'pdf'): Observable<Blob> {
    return this.blob(`/api/exports/fidelite${format === 'csv' ? '/csv' : ''}`);
  }

  recuCommande(id: number): Observable<Blob> {
    return this.http.get(`${API_URL}/api/commandes/${id}/recu/pdf`, { responseType: 'blob' });
  }

  ticketCaisse(date?: string): Observable<Blob> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.get(`${API_URL}/api/caisse/ticket`, { params, responseType: 'blob' });
  }

  private blob(base: string, debut?: string, fin?: string): Observable<Blob> {
    let params = new HttpParams();
    if (debut) params = params.set('debut', debut);
    if (fin) params = params.set('fin', fin);
    return this.http.get(`${API_URL}${base}`, { params, responseType: 'blob' });
  }
}

export function telechargerBlob(blob: Blob, nomFichier: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = nomFichier;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}