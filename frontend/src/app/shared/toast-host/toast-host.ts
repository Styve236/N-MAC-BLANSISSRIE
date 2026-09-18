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
      max-width: 380px;
    }
    .toast {
      display: flex;
      align-items: center;
      gap: 10px;
      background: #1e293b;
      color: #f8fafc;
      padding: 12px 14px;
      border-radius: 10px;
      box-shadow: 0 10px 25px rgba(0, 0, 0, 0.18);
      font-size: 14px;
      line-height: 1.4;
      animation: toast-in 0.2s ease-out;
      border-left: 4px solid #2563eb;
    }
    .toast.succes { border-left-color: #16a34a; }
    .toast.erreur { border-left-color: #dc2626; }
    .toast-fermer {
      background: transparent;
      border: none;
      color: #94a3b8;
      font-size: 18px;
      line-height: 1;
      cursor: pointer;
      flex-shrink: 0;
      padding: 0 2px;
    }
    .toast-fermer:hover { color: #fff; }
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