import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-button',
  standalone: true,
  template: ` <button
    [type]="type"
    [class.secondary]="variant === 'secondary'"
    (click)="clicked.emit()"
  >
    {{ label }}
  </button>`,
  styles: `
    button {
      border: none;
      border-radius: 8px;
      padding: 10px 18px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      background: #4f46e5;
      color: white;
      transition: background 0.15s ease;
    }
    button:hover {
      background: #4338ca;
    }
    button.secondary {
      background: #ffffff;
      color: #1f2333;
      border: 1px solid #e2e4ec;
    }
    button.secondary:hover {
      background: #f4f5f9;
    }
  `,
})
export class ButtonComponent {
  @Input() label = 'Botón';
  @Input() variant: 'primary' | 'secondary' = 'primary';
  @Input() type: 'button' | 'submit' = 'button';
  @Output() clicked = new EventEmitter<void>();
}
