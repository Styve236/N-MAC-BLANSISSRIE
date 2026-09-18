import { Component, inject, signal } from '@angular/core';
import { CfaPipe } from '../../shared/pipes/cfa.pipe';
import { DashboardResumeDTO } from '../../core/models/dashboard.model';
import { DashboardService } from '../../core/services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  imports: [CfaPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly service = inject(DashboardService);

  readonly data = signal<DashboardResumeDTO | null>(null);
  readonly enChargement = signal(true);

  constructor() {
    this.service.resume().subscribe({
      next: (r) => {
        this.data.set(r);
        this.enChargement.set(false);
      },
      error: () => this.enChargement.set(false),
    });
  }
}