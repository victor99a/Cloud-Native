import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loader',
  standalone: true,
  template: `
    @if (isLoading) {
      <div class="loader-wrap">
        <div class="spinner"></div>
        <span>Cargando...</span>
      </div>
    }
  `,
  styles: `
    .loader-wrap {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 16px 0;
      color: #6b7086;
      font-size: 14px;
    }
    .spinner {
      width: 18px;
      height: 18px;
      border: 2px solid #e2e4ec;
      border-top-color: #4f46e5;
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
}
