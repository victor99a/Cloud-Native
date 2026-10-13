import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { LoaderComponent } from '../../../shared/components/loader/loader.component';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  imports: [LoaderComponent],
  template: `
    <div class="callback">
      <app-loader [isLoading]="true" label="Procesando inicio de sesión..." />
    </div>
  `,
  styles: `
    .callback {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
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
