import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';
import { MetricaAnalitica } from '../models/metrica.model';

@Injectable({ providedIn: 'root' })
export class AnaliticaStoreService {
  private readonly api = inject(ApiService);

  private readonly _metricas = signal<MetricaAnalitica[]>([]);
  private readonly _isLoading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly metricas = this._metricas.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  cargar(): void {
    this._isLoading.set(true);
    this._error.set(null);
    this.api.get<MetricaAnalitica[]>('/analitica/resumen').subscribe({
      next: (metricas) => {
        this._metricas.set(metricas);
        this._isLoading.set(false);
      },
      error: () => {
        this._error.set('No se pudieron cargar las métricas de analítica.');
        this._isLoading.set(false);
      },
    });
  }
}
