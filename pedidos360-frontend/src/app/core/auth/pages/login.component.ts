import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  template: `
    <main class="login-container">
      <div class="login-card">
        <div class="logo" aria-hidden="true">
          <span class="logo-mark material-symbols-outlined">local_shipping</span>
        </div>

        <h1>Pedidos360</h1>
        <p class="subtitle">
          Gestión de pedidos, productos y catálogo. Inicia sesión con tu cuenta
          institucional para continuar.
        </p>

        <button class="login-button" type="button" (click)="login()">
          <svg viewBox="0 0 23 23" width="20" height="20" aria-hidden="true">
            <rect x="1" y="1" width="10" height="10" fill="#f25022" />
            <rect x="12" y="1" width="10" height="10" fill="#7fba00" />
            <rect x="1" y="12" width="10" height="10" fill="#00a4ef" />
            <rect x="12" y="12" width="10" height="10" fill="#ffb900" />
          </svg>
          Iniciar sesión con Microsoft
        </button>

        <p class="footer-note">
          Usa tu cuenta institucional (Azure AD / Entra ID).
        </p>
      </div>
    </main>
  `,
  styles: `
    .login-container {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 24px 16px;
      background:
        radial-gradient(1000px 500px at 10% -10%, var(--color-primary-soft), transparent 60%),
        var(--color-bg);
    }

    .login-card {
      width: 100%;
      max-width: 400px;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-md);
      padding: clamp(28px, 6vw, 40px);
      text-align: center;
    }

    .logo {
      display: flex;
      justify-content: center;
      margin-bottom: 16px;
    }

    .logo-mark {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 56px;
      height: 56px;
      border-radius: var(--radius);
      background: var(--color-primary);
      color: #fff;
      font-size: 30px;
    }

    h1 {
      font-size: 24px;
      margin-bottom: 8px;
    }

    .subtitle {
      color: var(--color-text-muted);
      font-size: 14px;
      margin-bottom: 24px;
    }

    .login-button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      width: 100%;
      min-height: 48px;
      padding: 12px 20px;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      background: var(--color-surface);
      color: var(--color-text);
      font-size: 15px;
      font-weight: 600;
      font-family: inherit;
      cursor: pointer;
      transition: background var(--transition), box-shadow var(--transition);
    }

    .login-button:hover {
      background: var(--color-bg);
      box-shadow: var(--shadow);
    }

    .footer-note {
      margin: 20px 0 0;
      font-size: 12px;
      color: var(--color-text-muted);
    }
  `,
})
export class LoginComponent {
  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {}

  login(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
      return;
    }
    this.authService.login();
  }
}
