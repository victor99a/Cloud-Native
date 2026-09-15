import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav>
      <span class="brand">Pedidos360</span>
      <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
      <a routerLink="/pedidos" routerLinkActive="active">Pedidos</a>
      <a routerLink="/productos" routerLinkActive="active">Productos</a>

      <div class="spacer"></div>

      @if (auth.account; as account) {
        <span class="user">{{ account.username }}</span>
      }
      <button type="button" class="logout-btn" (click)="auth.logout()">Cerrar sesión</button>
    </nav>
  `,
  styles: `
    nav {
      display: flex;
      align-items: center;
      gap: 20px;
      padding: 16px 24px;
      background: #ffffff;
      border-bottom: 1px solid #e2e4ec;
      margin-bottom: 24px;
    }
    .brand {
      font-weight: 700;
      color: #4f46e5;
      margin-right: 12px;
    }
    a {
      color: #6b7086;
      text-decoration: none;
      font-size: 14px;
      font-weight: 500;
      padding: 6px 10px;
      border-radius: 6px;
    }
    a:hover {
      background: #f4f5f9;
      color: #1f2333;
    }
    a.active {
      color: #4f46e5;
      background: #eef2ff;
    }
    .spacer {
      flex: 1;
    }
    .user {
      font-size: 13px;
      color: #6b7086;
    }
    .logout-btn {
      border: 1px solid #e2e4ec;
      background: white;
      color: #1f2333;
      font-size: 13px;
      font-weight: 500;
      padding: 6px 12px;
      border-radius: 6px;
      cursor: pointer;
    }
    .logout-btn:hover {
      background: #fee2e2;
      border-color: #fca5a5;
      color: #dc2626;
    }
  `,
})
export class NavbarComponent {
  readonly auth = inject(AuthService);
}
