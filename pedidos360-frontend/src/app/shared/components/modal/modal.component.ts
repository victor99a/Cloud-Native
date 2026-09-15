import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-modal',
  standalone: true,
  template: `
    @if (isOpen) {
      <div class="modal-overlay" (click)="close.emit()">
        <div class="modal-box" (click)="$event.stopPropagation()">
          <header>
            <h2>{{ title }}</h2>
            <button class="close-btn" (click)="close.emit()">✕</button>
          </header>

          <div class="modal-content">
            <ng-content></ng-content>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .modal-overlay {
      position: fixed;
      inset: 0;
      background: rgba(15, 15, 35, 0.45);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 100;
    }
    .modal-box {
      background: white;
      padding: 20px 24px;
      border-radius: 12px;
      min-width: 320px;
      max-width: 90vw;
      box-shadow: 0 10px 30px rgba(15, 15, 35, 0.2);
    }
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }
    header h2 {
      font-size: 18px;
      margin: 0;
    }
    .close-btn {
      border: none;
      background: transparent;
      cursor: pointer;
      font-size: 16px;
      color: #6b7086;
      border-radius: 6px;
      padding: 4px 8px;
    }
    .close-btn:hover {
      background: #f4f5f9;
      color: #1f2333;
    }
    .modal-content {
      color: #1f2333;
    }
  `,
})
export class ModalComponent {
  @Input() title = '';
  @Input() isOpen = false;
  @Output() close = new EventEmitter<void>();
}
