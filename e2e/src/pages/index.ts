import { Page, Locator } from '@playwright/test';

export class LoginPage {
  readonly page: Page;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly tabRegister: Locator;
  readonly roleSelect: Locator;
  readonly fullNameInput: Locator;
  readonly cpfInput: Locator;
  readonly phoneInput: Locator;
  readonly submitRegisterButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.emailInput = page.locator('input[type="email"]');
    this.passwordInput = page.locator('input[type="password"]');
    this.loginButton = page.locator('button:has-text("Entrar")');
    this.tabRegister = page.locator('button:has-text("Criar Conta")');
    this.roleSelect = page.locator('select[name="role"]');
    this.fullNameInput = page.locator('input[name="fullName"]');
    this.cpfInput = page.locator('input[name="cpf"]');
    this.phoneInput = page.locator('input[name="phone"]');
    this.submitRegisterButton = page.locator('button:has-text("Solicitar Cadastro")');
  }

  async goto() {
    await this.page.goto('/auth/login');
  }

  async login(email: string, pass: string) {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(pass);
    await this.loginButton.click();
  }
}

export class GaragePage {
  readonly page: Page;
  readonly newInspectionBtn: Locator;
  readonly plateBadge: Locator;
  readonly odometerText: Locator;
  readonly recentList: Locator;

  constructor(page: Page) {
    this.page = page;
    this.newInspectionBtn = page.locator('button:has-text("Reportar Defeito"), button:has-text("Nova Vistoria")');
    this.plateBadge = page.locator('[data-testid="motorcycle-plate"]');
    this.odometerText = page.locator('[data-testid="odometer-km"]');
    this.recentList = page.locator('[data-testid="recent-inspections"]');
  }

  async goto() {
    await this.page.goto('/rider/garage');
  }
}

export class InspectionWizardPage {
  readonly page: Page;
  readonly cameraInput: Locator;
  readonly contextReceived: Locator;
  readonly contextWear: Locator;
  readonly contextAccident: Locator;
  readonly riskSafe: Locator;
  readonly riskCritical: Locator;
  readonly odometerInput: Locator;
  readonly notesInput: Locator;
  readonly submitButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.cameraInput = page.locator('input[type="file"]');
    this.contextReceived = page.locator('button:has-text("Já recebi a moto assim")');
    this.contextWear = page.locator('button:has-text("Gastou no uso normal")');
    this.contextAccident = page.locator('button:has-text("Tive queda / batida")');
    this.riskSafe = page.locator('button:has-text("Consigo continuar rodando")');
    this.riskCritical = page.locator('button:has-text("Risco de segurança / Moto parada")');
    this.odometerInput = page.locator('input[name="odometer"]');
    this.notesInput = page.locator('textarea[name="notes"]');
    this.submitButton = page.locator('button:has-text("Enviar para Análise")');
  }
}

export class MechanicDeskPage {
  readonly page: Page;
  readonly queueTable: Locator;
  readonly photoZoom: Locator;
  readonly verdictSelect: Locator;
  readonly resolveButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.queueTable = page.locator('[data-testid="mechanic-queue-table"]');
    this.photoZoom = page.locator('[data-testid="photo-zoom-container"]');
    this.verdictSelect = page.locator('select[name="verdict"]');
    this.resolveButton = page.locator('button:has-text("Finalizar Laudo")');
  }
}
