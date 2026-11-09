import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, map, tap, throwError } from 'rxjs';
import {
  AuthUser,
  LoginRequest,
  LoginResponse,
  PublicConfigResponse,
  RegisterRequest,
  RegisterResponse,
  UserRole,
} from '../models/auth.models';
import { ProblemDetail } from '../models/problem-detail.model';

const TOKEN_KEY = 'mottu_access_token';
const USER_KEY = 'mottu_current_user';

export const SEED_CREDENTIALS = {
  VITRINE: {
    RIDER: { email: 'entregador@mottu-ops.local', password: 'Mottu@2026!' },
    MECHANIC: { email: 'mecanico@mottu-ops.local', password: 'Mottu@2026!' },
    MANAGER: { email: 'gestor@mottu-ops.local', password: 'Mottu@2026!' },
  },
  LIVE: {
    RIDER: { email: 'entregador.real@mottu-ops.local', password: 'Mottu@2026!' },
    MECHANIC: { email: 'mecanico.real@mottu-ops.local', password: 'Mottu@2026!' },
    MANAGER: { email: 'gestor.real@mottu-ops.local', password: 'Mottu@2026!' },
  },
  // Retrocompatibilidade
  MANAGER: { email: 'gestor@mottu-ops.local', password: 'Mottu@2026!' },
  MECHANIC: { email: 'mecanico@mottu-ops.local', password: 'Mottu@2026!' },
  RIDER: { email: 'entregador@mottu-ops.local', password: 'Mottu@2026!' },
} as const;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http: HttpClient | null;
  private router: Router | null;

  // Reactive State via Angular 19 Signals
  currentUser = signal<AuthUser | null>(this.getStoredUser());
  accessToken = signal<string | null>(this.getStoredToken());
  publicDemoMode = signal<boolean>(true); // Defaults to true for instant demo fast-switch
  sessionMode = signal<'VITRINE' | 'LIVE'>(this.getStoredMode());

  isAuthenticated = computed(() => !!this.accessToken() && !!this.currentUser());
  userRole = computed(() => this.currentUser()?.role ?? null);
  isManager = computed(() => this.userRole() === 'MANAGER');
  isMechanic = computed(() => this.userRole() === 'MECHANIC');
  isRider = computed(() => this.userRole() === 'RIDER');

  isLiveMode = computed(() => this.sessionMode() === 'LIVE' || (this.currentUser()?.fullName?.includes('Real') ?? false));
  modeLabel = computed(() => this.isLiveMode() ? 'Operação Real' : 'Modo Demonstração');

  constructor(
    http?: HttpClient,
    router?: Router
  ) {
    try {
      this.http = http ?? inject(HttpClient, { optional: true });
    } catch {
      this.http = http ?? null;
    }
    try {
      this.router = router ?? inject(Router, { optional: true });
    } catch {
      this.router = router ?? null;
    }

    if (this.http) {
      this.checkPublicConfig();
    }
  }

  checkPublicConfig(): void {
    if (!this.http) return;
    this.http.get<PublicConfigResponse>('/api/v1/auth/public-config')
      .subscribe({
        next: (config) => this.publicDemoMode.set(config.publicDemoMode),
        error: () => this.publicDemoMode.set(true), // Fallback true in offline/local
      });
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    const http = this.http;
    if (!http) {
      return throwError(() => ({
        title: 'HTTP Client Indisponível',
        status: 500,
        detail: 'Cliente HTTP não configurado.',
      } as ProblemDetail));
    }

    return http.post<LoginResponse>('/api/v1/auth/login', credentials).pipe(
      map((res) => {
        this.setSession(res);
        return res;
      }),
      catchError((error: HttpErrorResponse) => {
        return throwError(() => this.mapProblemDetail(error));
      })
    );
  }

  register(data: RegisterRequest): Observable<RegisterResponse> {
    const http = this.http;
    if (!http) {
      return throwError(() => ({
        title: 'HTTP Client Indisponível',
        status: 500,
        detail: 'Cliente HTTP não configurado.',
      } as ProblemDetail));
    }

    return http.post<RegisterResponse>('/api/v1/auth/register', data).pipe(
      catchError((error: HttpErrorResponse) => {
        return throwError(() => this.mapProblemDetail(error));
      })
    );
  }

  demoLogin(role: UserRole): Observable<LoginResponse> {
    return this.demoLoginMode('VITRINE', role);
  }

  demoLoginMode(mode: 'VITRINE' | 'LIVE', role: UserRole): Observable<LoginResponse> {
    const creds = SEED_CREDENTIALS[mode][role];
    this.sessionMode.set(mode);
    try {
      localStorage.setItem('mottu_session_mode', mode);
    } catch {
      // Storage fallback
    }
    return this.login(creds).pipe(
      map((res) => {
        this.navigateForRole(role);
        return res;
      })
    );
  }

  switchRole(role: UserRole, targetMode?: 'VITRINE' | 'LIVE'): Observable<LoginResponse> {
    const mode = targetMode ?? (this.isLiveMode() ? 'LIVE' : 'VITRINE');
    return this.demoLoginMode(mode, role);
  }

  setSessionMode(mode: 'VITRINE' | 'LIVE'): Observable<LoginResponse> {
    const currentRole = this.userRole() || 'MANAGER';
    return this.demoLoginMode(mode, currentRole);
  }

  logout(): void {
    this.clearSession();
    if (this.router) {
      this.router.navigate(['/login']);
    }
  }

  navigateForRole(role: UserRole): void {
    const target = role === 'MANAGER'
      ? '/manager/approvals'
      : (role === 'MECHANIC' ? '/mechanic/queue' : '/rider/garage');

    if (!this.router) {
      if (typeof window !== 'undefined') {
        window.location.href = target;
      }
      return;
    }

    const navRes = this.router.navigate([target]);
    if (navRes && typeof (navRes as any).then === 'function') {
      (navRes as Promise<boolean>).then((navigated) => {
        if (!navigated && typeof window !== 'undefined' && window.location.pathname !== target) {
          window.location.href = target;
        }
      }).catch(() => {
        if (typeof window !== 'undefined') {
          window.location.href = target;
        }
      });
    }
  }

  private setSession(res: LoginResponse): void {
    const user: AuthUser = {
      userId: res.userId,
      fullName: res.fullName,
      role: res.role,
    };
    const isLive = res.fullName?.includes('Real');
    if (isLive) {
      this.sessionMode.set('LIVE');
    }
    try {
      localStorage.setItem(TOKEN_KEY, res.accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify(user));
      if (isLive) {
        localStorage.setItem('mottu_session_mode', 'LIVE');
      }
    } catch {
      // Storage unavailable fallback
    }
    this.accessToken.set(res.accessToken);
    this.currentUser.set(user);
  }

  private clearSession(): void {
    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    } catch {
      // Ignored
    }
    this.accessToken.set(null);
    this.currentUser.set(null);
  }

  private getStoredToken(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  }

  private getStoredUser(): AuthUser | null {
    try {
      const data = localStorage.getItem(USER_KEY);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  }

  private getStoredMode(): 'VITRINE' | 'LIVE' {
    try {
      const mode = localStorage.getItem('mottu_session_mode');
      return mode === 'LIVE' ? 'LIVE' : 'VITRINE';
    } catch {
      return 'VITRINE';
    }
  }

  private mapProblemDetail(error: HttpErrorResponse): ProblemDetail {
    if (error.error && typeof error.error === 'object' && 'title' in error.error) {
      return error.error as ProblemDetail;
    }
    return {
      title: 'Erro de Comunicação',
      status: error.status || 500,
      detail: error.error?.detail || error.error?.message || error.message || 'Falha ao processar solicitação.',
    };
  }
}
