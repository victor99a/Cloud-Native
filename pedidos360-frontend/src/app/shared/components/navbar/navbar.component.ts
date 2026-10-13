import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

const ROLES_PRODUCTOS = ['ADMIN', 'OPERADOR'];

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar" aria-label="Navegación principal">
      <a class="brand" routerLink="/dashboard" (click)="closeMenu()">Pedidos360</a>

      <button
        class="menu-toggle"
        type="button"
        [attr.aria-label]="menuOpen() ? 'Cerrar menú' : 'Abrir menú'"
        [attr.aria-expanded]="menuOpen()"
        aria-controls="nav-links"
        (click)="menuOpen.set(!menuOpen())"
      >
        <span></span>
        <span></span>
        <span></span>
      </button>

      <div class="nav-links" id="nav-links" [class.open]="menuOpen()">
        <a routerLink="/dashboard" routerLinkActive="active" (click)="closeMenu()">Dashboard</a>
        <a routerLink="/pedidos" routerLinkActive="active" (click)="closeMenu()">Pedidos</a>
        @if (puedeVerProductos()) {
          <a routerLink="/productos" routerLinkActive="active" (click)="closeMenu()">Productos</a>
        }

        <span class="nav-spacer"></span>

        @if (nombreUsuario()) {
          <span class="user" [title]="nombreUsuario()">{{ nombreUsuario() }}</span>
        }
        <button class="logout" type="button" (click)="logout()">Cerrar sesión</button>
      </div>
    </nav>
  `,
  styles: `
    .navbar {
      position: sticky;
      top: 0;
      z-index: 50;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px 20px;
      padding: 12px clamp(16px, 4vw, 24px);
      background: var(--color-surface);
      border-bottom: 1px solid var(--color-border);
      box-shadow: var(--shadow);
    }

    .brand {
      font-weight: 700;
      font-size: 18px;
      color: var(--color-primary);
      text-decoration: none;
      margin-right: 4px;
    }

    .nav-links {
      display: flex;
      align-items: center;
      gap: 4px;
      flex: 1;
      min-width: 0;
    }

    .nav-links a {
      color: var(--color-text-muted);
      text-decoration: none;
      font-size: 14px;
      font-weight: 500;
      padding: 8px 12px;
      border-radius: var(--radius-sm);
      transition:
        background var(--transition),
        color var(--transition);
      white-space: nowrap;
    }

    .nav-links a:hover {
      background: var(--color-bg);
      color: var(--color-text);
    }

    .nav-links a.active {
      color: var(--color-primary);
      background: var(--color-primary-soft);
    }

    .nav-spacer {
      flex: 1;
    }

    .user {
      font-size: 13px;
      color: var(--color-text-muted);
      max-width: 180px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .logout {
      border: 1px solid var(--color-border);
      background: var(--color-surface);
      color: var(--color-text);
      border-radius: var(--radius-sm);
      padding: 8px 14px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: background var(--transition);
    }

    .logout:hover {
      background: var(--color-bg);
    }

    /* Botón hamburguesa (solo móvil) */
    .menu-toggle {
      display: none;
      flex-direction: column;
      justify-content: center;
      gap: 5px;
      width: 44px;
      height: 44px;
      padding: 10px;
      margin-left: auto;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      background: var(--color-surface);
      cursor: pointer;
    }

    .menu-toggle span {
      display: block;
      height: 2px;
      width: 100%;
      background: var(--color-text);
      border-radius: 2px;
      transition: transform var(--transition), opacity var(--transition);
    }

    @media (max-width: 768px) {
      .menu-toggle {
        display: flex;
      }

      .nav-links {
        display: none;
        flex-basis: 100%;
        flex-direction: column;
        align-items: stretch;
        gap: 4px;
        padding-top: 8px;
        border-top: 1px solid var(--color-border);
      }

      .nav-links.open {
        display: flex;
      }

      .nav-links a,
      .logout {
        width: 100%;
        text-align: left;
        padding: 12px 14px;
      }

      .nav-spacer {
        display: none;
      }

      .user {
        padding: 8px 14px;
        border-top: 1px solid var(--color-border);
        margin-top: 8px;
        max-width: none;
      }

      /* Convierte la hamburguesa en una "X" al abrir */
      .menu-toggle[aria-expanded='true'] span:nth-child(1) {
        transform: translateY(7px) rotate(45deg);
      }
      .menu-toggle[aria-expanded='true'] span:nth-child(2) {
        opacity: 0;
      }
      .menu-toggle[aria-expanded='true'] span:nth-child(3) {
        transform: translateY(-7px) rotate(-45deg);
      }
    }
  `,
})
export class NavbarComponent {
  private readonly authService = inject(AuthService);

  readonly menuOpen = signal(false);

  nombreUsuario(): string {
    return this.authService.userName;
  }

  puedeVerProductos(): boolean {
    const roles = this.authService.userRoles.map((r) => r.toUpperCase());
    return roles.some((r) => ROLES_PRODUCTOS.includes(r));
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  logout(): void {
    this.authService.logout();
  }
}
