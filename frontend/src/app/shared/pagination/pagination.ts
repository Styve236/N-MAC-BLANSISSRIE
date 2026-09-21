import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../icon/icon';

@Component({
  selector: 'app-pagination',
  imports: [FormsModule, Icon],
  template: `
    @if (totalPages() > 1) {
      <div class="pagination">
        @if (tailles().length > 0) {
          <div class="pg-tailles">
            <span>Afficher</span>
            <select [ngModel]="pageSize()" (ngModelChange)="changerTaille($event)" [disabled]="desactive()">
              @for (t of tailles(); track t) {
                <option [ngValue]="t">{{ t }}</option>
              }
            </select>
            <span>par page</span>
          </div>
        }

        <div class="pg-boutons">
          <button class="pg-btn" (click)="precedente()" [disabled]="desactive() || page() === 0" title="Page précédente">
            <app-icon nom="chevron-left" [taille]="16" />
          </button>
          @for (p of pages(); track p) {
            <button
              class="pg-btn"
              [class.pg-actif]="p === page()"
              (click)="allerPage(p)"
              [disabled]="desactive()"
            >
              {{ p + 1 }}
            </button>
          }
          <button
            class="pg-btn"
            (click)="suivante()"
            [disabled]="desactive() || page() >= totalPages() - 1"
            title="Page suivante"
          >
            <app-icon nom="chevron-right" [taille]="16" />
          </button>
        </div>

        <span class="pg-infos">Page {{ page() + 1 }} / {{ totalPages() }}</span>
      </div>
    }
  `,
  styles: `
    .pagination {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 14px;
      flex-wrap: wrap;
      margin-top: 14px;
      font-size: 13px;
    }
    .pg-boutons {
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .pg-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 32px;
      height: 32px;
      padding: 0 8px;
      border: 1px solid var(--line);
      border-radius: 8px;
      background: var(--panel);
      color: var(--ink);
      font-size: 13px;
      cursor: pointer;
      transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
    }
    .pg-btn:hover:not(:disabled) {
      border-color: var(--indigo);
      color: var(--indigo);
    }
    .pg-btn:disabled {
      opacity: 0.45;
      cursor: not-allowed;
    }
    .pg-btn.pg-actif {
      background: var(--indigo);
      border-color: var(--indigo);
      color: #fff;
      font-weight: 600;
    }
    .pg-infos {
      color: var(--ink-soft);
      white-space: nowrap;
    }
    .pg-tailles {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--ink-soft);
    }
    .pg-tailles select {
      padding: 5px 8px;
      border: 1px solid var(--line);
      border-radius: 8px;
      background: var(--panel);
      color: var(--ink);
      font-size: 13px;
    }
  `,
})
export class Pagination {
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly pageSize = input<number>(20);
  readonly tailles = input<number[]>([]);
  readonly desactive = input<boolean>(false);

  readonly pageChange = output<number>();
  readonly pageSizeChange = output<number>();

  precedente(): void {
    if (this.page() > 0) this.allerPage(this.page() - 1);
  }

  suivante(): void {
    if (this.page() < this.totalPages() - 1) this.allerPage(this.page() + 1);
  }

  allerPage(p: number): void {
    if (p === this.page() || this.desactive() || p < 0 || p >= this.totalPages()) return;
    this.pageChange.emit(p);
  }

  changerTaille(t: number): void {
    if (t === this.pageSize()) return;
    this.pageSizeChange.emit(t);
  }

  protected pages(): number[] {
    const total = this.totalPages();
    const cur = this.page();
    const max = 5;
    let debut = Math.max(0, cur - Math.floor(max / 2));
    const fin = Math.min(total, debut + max);
    debut = Math.max(0, fin - max);
    return Array.from({ length: fin - debut }, (_, i) => debut + i);
  }
}