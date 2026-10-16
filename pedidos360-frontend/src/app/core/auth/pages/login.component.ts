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
          <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
            <path
              fill="currentColor"
              d="M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 2.18 7 3.11v4.71c0 4.25-2.76 8.31-7 9.79-4.24-1.48-7-5.54-7-9.79V6.29l7-3.11zm-1 5.07v4.5h2v-4.5h-2zm0 6v2h2v-2h-2z"
            />
          </svg>
          Iniciar sesión
        </button>

        <p class="footer-note">
          Autenticación centralizada con Keycloak (OAuth 2.0 / OpenID Connect).
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
