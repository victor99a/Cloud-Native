import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  template: `<p class="callback-message">Procesando inicio de sesión...</p>`,
  styles: `
    .callback-message {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100vh;
      font-family: system-ui, sans-serif;
      color: #4b5563;
    }
  `,
})
export class AuthCallbackComponent {
  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/login']);
    }
  }
}