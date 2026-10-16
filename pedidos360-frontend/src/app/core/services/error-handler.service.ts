import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { LoggerService } from './logger.service';

@Injectable({ providedIn: 'root' })
export class ErrorHandlerService {
  constructor(
    private readonly router: Router,
    private readonly logger: LoggerService,
  ) {}

  handle(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      return this.handleHttpError(error);
    }
    this.logger.error('Error no controlado', error);
    return 'Ocurrió un error inesperado';
  }

  private handleHttpError(error: HttpErrorResponse): string {
    this.logger.error(`[HTTP ${error.status}] ${error.url ?? ''}`, error.message);
    switch (error.status) {
      case 401:
        this.router.navigate(['/login']);
        return 'Tu sesión expiró. Inicia sesión nuevamente';
      case 403:
        return 'No tienes permisos para realizar esta acción';
      case 404:
        return 'El recurso solicitado no existe';
      case 400:
        return this.extractMessage(error) ?? 'La solicitud es inválida';
      case 0:
        return 'No se pudo conectar con el servidor';
      default:
        return 'Error del servidor. Intenta más tarde';
    }
  }

  private extractMessage(error: HttpErrorResponse): string | null {
    const body: unknown = error.error;
    if (body && typeof body === 'object' && 'message' in body) {
      const message = (body as { message?: unknown }).message;
      if (typeof message === 'string') {
        return message;
      }
    }
    return null;
  }
}