import { Injectable, signal } from '@angular/core';

export type TypeToast = 'succes' | 'erreur' | 'info';

export interface Toast {
  id: number;
  type: TypeToast;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);

  private compteur = 0;

  succes(message: string): void {
    this.ajouter('succes', message);
  }

  erreur(message: string): void {
    this.ajouter('erreur', message);
  }

  info(message: string): void {
    this.ajouter('info', message);
  }

  retirer(id: number): void {
    this.toasts.update((liste) => liste.filter((t) => t.id !== id));
  }

  private ajouter(type: TypeToast, message: string): void {
    const id = ++this.compteur;
    this.toasts.update((liste) => [...liste, { id, type, message }]);
    setTimeout(() => this.retirer(id), 5000);
  }
}