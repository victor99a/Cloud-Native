import { Injectable } from '@angular/core';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { AccountInfo, InteractionStatus, SilentRequest } from '@azure/msal-browser';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly accessToken = new BehaviorSubject<string>('');

  readonly accessToken$ = this.accessToken.asObservable();

  readonly loginStatus$: Observable<boolean>;

  constructor(
    private readonly msalService: MsalService,
    private readonly msalBroadcastService: MsalBroadcastService,
  ) {
    this.loginStatus$ = this.msalBroadcastService.inProgress$.pipe(
      map((status) => status === InteractionStatus.None),
    );
  }

  get account(): AccountInfo | undefined {
    return (
      this.msalService.instance.getActiveAccount() ??
      this.msalService.instance.getAllAccounts()[0]
    );
  }

  isAuthenticated(): boolean {
    return this.msalService.instance.getAllAccounts().length > 0;
  }

  get userRoles(): string[] {
    const claims = this.account?.idTokenClaims as Record<string, unknown> | undefined;
    const idRoles = claims?.['roles'];
    if (Array.isArray(idRoles)) {
      return idRoles.map(String);
    }
    return this.decodeRoles(this.accessToken.value);
  }

  getAccessToken(): string {
    return this.accessToken.value;
  }

  proposeAccessToken(token: string): void {
    if (token) {
      this.accessToken.next(token);
    }
  }

  async initialize(): Promise<void> {
    await this.msalService.instance.handleRedirectPromise();
    const account = this.account;
    if (account) {
      this.msalService.instance.setActiveAccount(account);
      await this.acquireTokenSilently(account);
    }
  }

  login(): void {
    this.msalService.loginRedirect({ scopes: environment.azureAd.scopes });
  }

  logout(): void {
    this.msalService.logoutRedirect({
      postLogoutRedirectUri: environment.azureAd.postLogoutRedirectUri,
    });
  }

  async acquireTokenSilently(account?: AccountInfo): Promise<string | null> {
    const target = account ?? this.account;
    if (!target) {
      return null;
    }
    const request: SilentRequest = {
      scopes: environment.azureAd.scopes,
      account: target,
    };
    try {
      const response = await this.msalService.instance.acquireTokenSilent(request);
      this.accessToken.next(response.accessToken);
      return response.accessToken;
    } catch {
      return null;
    }
  }

  private decodeRoles(token: string): string[] {
    const payload = this.decodePayload(token);
    const roles = payload?.['roles'];
    return Array.isArray(roles) ? roles.map(String) : [];
  }

  private decodePayload(token: string): Record<string, unknown> | null {
    if (!token) {
      return null;
    }
    try {
      const base64Url = token.split('.')[1];
      if (!base64Url) {
        return null;
      }
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const json = decodeURIComponent(
        atob(base64)
          .split('')
          .map((char) => `%${`00${char.charCodeAt(0).toString(16)}`.slice(-2)}`)
          .join(''),
      );
      return JSON.parse(json) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
}