import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { ManagerService } from '../../services/manager.service';
import { AnalyticsSummaryResponseDto } from '../../models/manager.models';
import { DesktopShellComponent } from '../../../../shared/layout/desktop-shell/desktop-shell.component';

export interface DailyInspectionBar {
  day: string;
  directPct: number;
  workshopPct: number;
  totalCount: number;
}

export interface VerdictBreakdownRow {
  label: string;
  subtitle: string;
  percentage: number;
  barClass: string;
}

@Component({
  selector: 'app-t10-analytics',
  standalone: true,
  imports: [CommonModule, RouterLink, DesktopShellComponent],
  templateUrl: './t10-analytics.component.html',
})
export class T10AnalyticsComponent implements OnInit {
  private managerService: ManagerService;
  private sanitizer: DomSanitizer;

  summary = signal<AnalyticsSummaryResponseDto | null>(null);
  safeEmbedUrl = signal<SafeResourceUrl | string | null>(null);
  isOfflineFallback = signal<boolean>(false);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  readonly dailyVolumeBars: DailyInspectionBar[] = [
    { day: 'Seg', directPct: 76, workshopPct: 24, totalCount: 42 },
    { day: 'Ter', directPct: 80, workshopPct: 20, totalCount: 38 },
    { day: 'Qua', directPct: 79, workshopPct: 21, totalCount: 45 },
    { day: 'Qui', directPct: 82, workshopPct: 18, totalCount: 51 },
    { day: 'Sex', directPct: 78, workshopPct: 22, totalCount: 49 },
    { day: 'Sáb', directPct: 84, workshopPct: 16, totalCount: 33 },
    { day: 'Dom', directPct: 86, workshopPct: 14, totalCount: 24 },
  ];

  readonly verdictBreakdown: VerdictBreakdownRow[] = [
    {
      label: 'Desgaste natural de uso (R$ 0,00)',
      subtitle: 'Pastilhas, cabos e relação cobertos pelo plano',
      percentage: 46,
      barClass: 'bg-[#05944F]',
    },
    {
      label: 'Pré-existente na retirada (R$ 0,00)',
      subtitle: 'Protegido pela janela de 48h ou 100 km',
      percentage: 29,
      barClass: 'bg-[#141414]',
    },
    {
      label: 'Dano por queda ou colisão',
      subtitle: 'Orçado pela tabela oficial da Loja Mottu',
      percentage: 25,
      barClass: 'bg-[#D97706]',
    },
  ];

  constructor(managerService?: ManagerService, sanitizer?: DomSanitizer) {
    try {
      this.managerService = managerService ?? inject(ManagerService);
    } catch {
      this.managerService = managerService as ManagerService;
    }
    try {
      this.sanitizer = sanitizer ?? inject(DomSanitizer);
    } catch {
      this.sanitizer = sanitizer as DomSanitizer;
    }
  }

  ngOnInit(): void {
    this.loadSummary();
  }

  loadSummary(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.managerService.getAnalyticsSummary().subscribe({
      next: (res) => {
        this.summary.set(res);
        this.isLoading.set(false);

        const url = res.lookerStudioEmbedUrl?.trim();
        if (url && url.length > 0) {
          this.safeEmbedUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url));
          this.isOfflineFallback.set(false);
        } else {
          this.safeEmbedUrl.set(null);
          this.isOfflineFallback.set(true);
        }
      },
      error: () => {
        this.errorMessage.set('Erro ao carregar indicadores executivos de inteligência operacional.');
        this.isLoading.set(false);
        // Display default fallback state so UI remains functional
        this.isOfflineFallback.set(true);
      },
    });
  }

  refresh(): void {
    this.loadSummary();
  }
}
