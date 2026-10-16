import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loader',
  standalone: true,
  template: `
    @if (isLoading) {
      <div class="loader-wrap" role="status" aria-live="polite">
        <div class="spinner" aria-hidden="true"></div>
        <span>{{ label }}</span>
      </div>
    }
  `,
  styles: `
    .loader-wrap {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      padding: 24px 0;
      color: var(--color-text-muted);
      font-size: 14px;
    }

    .spinner {
      width: 22px;
      height: 22px;
      border: 3px solid var(--color-border);
      border-top-color: var(--color-primary);
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }
  `,
})
export class LoaderComponent {
  @Input() isLoading = false;
  @Input() label = 'Cargando...';
}
