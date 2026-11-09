import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, SEED_CREDENTIALS } from '../../core/services/auth.service';
import { UserRole } from '../../core/models/auth.models';
import { ProblemDetail } from '../../core/models/problem-detail.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private authService: AuthService | null;
  private router: Router | null;

  // View state signals
  activeTab = signal<'LOGIN' | 'REGISTER'>('LOGIN');
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  pendingApprovalNotice = signal<boolean>(false);
  successMessage = signal<string | null>(null);
  quickAccessMode = signal<'VITRINE' | 'LIVE'>('VITRINE');

  // Demo mode signal from service
  publicDemoMode = computed(() => {
    if (!this.authService) return true;
    const pdm = this.authService.publicDemoMode;
    return typeof pdm === 'function' ? pdm() : true;
  });

  // Login form model
  loginEmail = signal<string>(SEED_CREDENTIALS.RIDER.email);
  loginPassword = signal<string>(SEED_CREDENTIALS.RIDER.password);

  // Register form model
  registerRole = signal<'RIDER' | 'MECHANIC'>('RIDER');
  registerFullName = signal<string>('');
  registerCpf = signal<string>('');
  registerPhone = signal<string>('');
  registerEmail = signal<string>('');
  registerPassword = signal<string>('');

  constructor(authService?: AuthService, router?: Router) {
    try {
      this.authService = authService ?? inject(AuthService, { optional: true });
    } catch {
      this.authService = authService ?? null;
    }
    try {
      this.router = router ?? inject(Router, { optional: true });
    } catch {
      this.router = router ?? null;
    }
  }

  setTab(tab: 'LOGIN' | 'REGISTER'): void {
    this.activeTab.set(tab);
    this.errorMessage.set(null);
    this.pendingApprovalNotice.set(false);
    this.successMessage.set(null);
  }

  onLogin(): void {
    if (!this.loginEmail() || !this.loginPassword()) {
      this.errorMessage.set('Preencha e-mail e senha para acessar.');
      return;
    }

    if (!this.authService) {
      this.errorMessage.set('Serviço de autenticação indisponível.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.pendingApprovalNotice.set(false);

    this.authService.login({
      email: this.loginEmail().trim(),
      password: this.loginPassword(),
    }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.authService?.navigateForRole(res.role);
      },
      error: (prob: ProblemDetail) => {
        this.isLoading.set(false);
        if (prob.status === 403) {
          this.pendingApprovalNotice.set(true);
        } else if (prob.status === 401) {
          this.errorMessage.set('E-mail ou senha inválidos.');
        } else {
          this.errorMessage.set(prob.detail || 'Falha ao autenticar.');
        }
      },
    });
  }

  onRegister(): void {
    if (
      !this.registerFullName() ||
      !this.registerCpf() ||
      !this.registerPhone() ||
      !this.registerEmail() ||
      !this.registerPassword()
    ) {
      this.errorMessage.set('Todos os campos são obrigatórios.');
      return;
    }

    if (!this.authService) {
      this.errorMessage.set('Serviço de autenticação indisponível.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.register({
      fullName: this.registerFullName().trim(),
      cpf: this.registerCpf().replace(/\D/g, ''),
      phone: this.registerPhone().replace(/\D/g, ''),
      email: this.registerEmail().trim(),
      password: this.registerPassword(),
      role: this.registerRole(),
    }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.successMessage.set(res.message);
        this.pendingApprovalNotice.set(true);
        this.activeTab.set('LOGIN');
      },
      error: (prob: ProblemDetail) => {
        this.isLoading.set(false);
        this.errorMessage.set(prob.detail || 'Não foi possível solicitar o cadastro.');
      },
    });
  }

  onDemoLogin(role: UserRole): void {
    if (!this.authService) {
      this.errorMessage.set('Serviço de autenticação indisponível.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.pendingApprovalNotice.set(false);

    this.authService.demoLogin(role).subscribe({
      next: () => {
        this.isLoading.set(false);
      },
      error: (prob: ProblemDetail) => {
        this.isLoading.set(false);
        this.errorMessage.set(prob.detail || 'Falha no login de demonstração.');
      },
    });
  }

  setQuickAccessMode(mode: 'VITRINE' | 'LIVE'): void {
    this.quickAccessMode.set(mode);
  }

  onQuickLogin(role: UserRole): void {
    this.onDemoLoginMode(this.quickAccessMode(), role);
  }

  onDemoLoginMode(mode: 'VITRINE' | 'LIVE', role: UserRole): void {
    if (!this.authService) {
      this.errorMessage.set('Serviço de autenticação indisponível.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.pendingApprovalNotice.set(false);

    this.authService.demoLoginMode(mode, role).subscribe({
      next: () => {
        this.isLoading.set(false);
      },
      error: (prob: ProblemDetail) => {
        this.isLoading.set(false);
        this.errorMessage.set(prob.detail || 'Falha no login de demonstração.');
      },
    });
  }
}
