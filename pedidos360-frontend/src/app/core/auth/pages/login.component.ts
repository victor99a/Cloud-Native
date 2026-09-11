import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  template: `
    <div class="login-container">
      <h1>Pedidos360</h1>
      <p>Inicia sesión con tu cuenta institucional para continuar.</p>
      <button type="button" class="login-button" (click)="login()">Iniciar sesión con Microsoft</button>
    </div>
  `,
  styles: `
    .login-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100vh;
      background: #f5f6fa;
      font-family: system-ui, sans-serif;
    }
    .login-button {
      margin-top: 16px;
      padding: 12px 24px;
      border: none;
      border-radius: 8px;
      background: #2f60ce;
      color: #fff;
      font-size: 15px;
      cursor: pointer;
    }
    .login-button:hover {
      background: #274ea8;
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