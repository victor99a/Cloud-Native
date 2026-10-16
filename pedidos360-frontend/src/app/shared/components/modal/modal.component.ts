import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';

@Component({
  selector: 'app-modal',
  standalone: true,
  template: `
    @if (isOpen) {
      <div
        class="modal-overlay"
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="title"
        (click)="close.emit()"
      >
        <div class="modal-box" (click)="$event.stopPropagation()">
          <header>
            <h2>{{ title }}</h2>
            <button #closeBtn class="close-btn" type="button" aria-label="Cerrar" (click)="close.emit()">
              ✕
            </button>
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
      background: rgba(15, 15, 35, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 100;
      padding: 16px;
    }

    .modal-box {
      background: var(--color-surface);
      border-radius: var(--radius-lg);
      width: 100%;
      max-width: 560px;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
      box-shadow: var(--shadow-lg);
    }

    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 24px 0;
    }

    header h2 {
      font-size: 18px;
      margin: 0;
    }

    .close-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border: none;
      background: transparent;
      cursor: pointer;
      font-size: 16px;
      color: var(--color-text-muted);
      border-radius: var(--radius-sm);
      transition: background var(--transition), color var(--transition);
    }

    .close-btn:hover {
      background: var(--color-bg);
      color: var(--color-text);
    }

    .modal-content {
      padding: 16px 24px 24px;
      overflow-y: auto;
      color: var(--color-text);
    }

    @media (max-width: 640px) {
      .modal-overlay {
        padding: 8px;
        align-items: flex-end;
      }

      .modal-box {
        max-width: 100%;
        max-height: 94vh;
        border-radius: var(--radius-lg) var(--radius-lg) 0 0;
      }

      header {
        padding: 16px 16px 0;
      }

      .modal-content {
        padding: 12px 16px 20px;
      }
    }
  `,
})
export class ModalComponent implements OnChanges, OnDestroy {
  @Input() title = '';
  @Input() isOpen = false;
  @Output() close = new EventEmitter<void>();

  @ViewChild('closeBtn') private readonly closeBtn?: ElementRef<HTMLButtonElement>;

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['isOpen']) {
      return;
    }
    if (this.isOpen) {
      document.body.style.overflow = 'hidden';
      // Enfoca el botón de cierre al abrir (accesibilidad)
      setTimeout(() => this.closeBtn?.nativeElement.focus());
    } else {
      document.body.style.overflow = '';
    }
  }

  ngOnDestroy(): void {
    document.body.style.overflow = '';
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isOpen) {
      this.close.emit();
    }
  }
}
