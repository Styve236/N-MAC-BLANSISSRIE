import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_URL } from '../config/constants';
import { Photo } from '../models/commande.model';

@Injectable({ providedIn: 'root' })
export class PhotoService {
  constructor(private readonly http: HttpClient) {}

  lister(ligneId: number): Observable<Photo[]> {
    return this.http.get<Photo[]>(`${API_URL}/api/lignes/${ligneId}/photos`);
  }

  uploader(ligneId: number, file: File, type?: string): Observable<Photo> {
    const formData = new FormData();
    formData.append('file', file);
    if (type) formData.append('type', type);
    return this.http.post<Photo>(`${API_URL}/api/lignes/${ligneId}/photos`, formData);
  }

  supprimer(idphoto: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/api/photos/${idphoto}`);
  }

  /**
   * Les fichiers photo exigent un jeton : un <img src> n'envoie pas l'en-tete
   * Authorization et afficherait une image cassee. On telecharge donc la photo en
   * blob (l'intercepteur ajoute le jeton) puis on pointe l'image sur une URL
   * locale. Les URL creees sont revoquees a la destruction du composant.
   */
  chargerFichier(idphoto: number): Observable<string> {
    return this.http
      .get(`${API_URL}/api/photos/${idphoto}/fichier`, { responseType: 'blob' })
      .pipe(map((blob) => URL.createObjectURL(blob)));
  }
}