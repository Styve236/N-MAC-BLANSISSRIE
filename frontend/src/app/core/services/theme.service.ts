import { Injectable, effect, signal } from '@angular/core';

export type Theme = 'clair' | 'sombre';

const CLE = 'nmac_theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<Theme>(this.initiale());

  constructor() {
    this.applique();
    effect(() => this.applique(this.theme()));
  }

  basculer(): void {
    const suivante = this.theme() === 'clair' ? 'sombre' : 'clair';
    this.theme.set(suivante);
    localStorage.setItem(CLE, suivante);
  }

  private applique(t?: Theme): void {
    document.documentElement.setAttribute('data-theme', (t ?? this.theme()) === 'sombre' ? 'dark' : 'light');
  }

  private initiale(): Theme {
    const stockee = localStorage.getItem(CLE);
    if (stockee === 'clair' || stockee === 'sombre') return stockee;
    try {
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'sombre' : 'clair';
    } catch {
      return 'clair';
    }
  }
}