import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-button',
  standalone: true,
  template: `
    <button
      [type]="type"
      [class.secondary]="variant === 'secondary'"
      [class.danger]="variant === 'danger'"
      [disabled]="disabled || loading"
      [attr.aria-busy]="loading || null"
      (click)="clicked.emit()"
    >
      @if (loading) {
        <span class="spinner" aria-hidden="true"></span>
      }
      <span>{{ label }}</span>
    </button>
  `,
  styles: `
    button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      border: none;
      border-radius: var(--radius-sm);
      padding: 10px 18px;
      min-height: 44px;
      font-size: 14px;
      font-weight: 600;
      font-family: inherit;
      cursor: pointer;
      background: var(--color-primary);
      color: #fff;
      transition: background var(--transition);
    }

    button:hover:not(:disabled) {
      background: var(--color-primary-hover);
    }

    button:disabled {
      opacity: 0.55;
      cursor: not-allowed;
    }

    button.secondary {
      background: var(--color-surface);
      color: var(--color-text);
      border: 1px solid var(--color-border);
    }

    button.secondary:hover:not(:disabled) {
      background: var(--color-bg);
    }

    button.danger {
      background: var(--color-danger);
    }

    button.danger:hover:not(:disabled) {
      background: #b91c1c;
    }

    .spinner {
      width: 16px;
      height: 16px;
      border: 2px solid rgba(127, 127, 127, 0.3);
      border-top-color: currentColor;
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
export class ButtonComponent {
  @Input() label = 'Botón';
  @Input() variant: 'primary' | 'secondary' | 'danger' = 'primary';
  @Input() type: 'button' | 'submit' = 'button';
  @Input() disabled = false;
  @Input() loading = false;
  @Output() clicked = new EventEmitter<void>();
}
