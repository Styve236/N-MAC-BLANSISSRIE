import { Component, inject } from '@angular/core';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-toast-host',
  template: `
    <div class="toast-host" aria-live="polite">
      @for (t of liste(); track t.id) {
        <div class="toast" [class.erreur]="t.type === 'erreur'" [class.succes]="t.type === 'succes'">
          <span>{{ t.message }}</span>
          <button class="toast-fermer" (click)="fermer(t.id)" aria-label="Fermer">&times;</button>
        </div>
      }
    </div>
  `,
  styles: `
    .toast-host {
      position: fixed;
      top: 16px;
      right: 16px;
      z-index: 1000;
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-width: min(380px, calc(100vw - 32px));
    }
    .toast {
      display: flex;
      align-items: center;
      gap: 10px;
      background: var(--panel);
      color: var(--ink);
      padding: 12px 14px;
      border: 1px solid var(--line);
      border-radius: var(--rayon-champ);
      box-shadow:
        0 10px 15px -3px rgb(15 23 42 / 0.12),
        0 4px 6px -4px rgb(15 23 42 / 0.1);
      font-size: 13px;
      font-weight: 500;
      line-height: 1.4;
      animation: toast-in 0.2s ease-out;
      border-left: 4px solid var(--primaire);
    }
    .toast.succes { border-left-color: var(--succes); }
    .toast.erreur { border-left-color: var(--danger); }
    .toast-fermer {
      background: transparent;
      border: none;
      color: var(--ink-muted);
      font-size: 18px;
      line-height: 1;
      cursor: pointer;
      flex-shrink: 0;
      padding: 0 2px;
    }
    .toast-fermer:hover { color: var(--ink); }
    @keyframes toast-in {
      from { opacity: 0; transform: translateY(-8px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `,
})
export class ToastHost {
  private readonly toastsSvc = inject(ToastService);

  protected readonly liste = this.toastsSvc.toasts;

  fermer(id: number): void {
    this.toastsSvc.retirer(id);
  }
}