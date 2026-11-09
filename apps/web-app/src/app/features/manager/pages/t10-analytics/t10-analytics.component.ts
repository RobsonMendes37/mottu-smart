import { Component, OnInit, computed, inject, signal } from '@angular/core';
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
  count: number;
  barClass: string;
  dotColor: string;
}

export interface YardBiSnapshot {
  key: string;
  label: string;
  totalInspections: number;
  aiPrecisionPct: string;
  avgSlaSeconds: string;
  auditedRevenueBrl: string;
  naturalWearCount: number;
  preExistingCount: number;
  badUseCount: number;
}

export interface FleetModelAnalyticsRow {
  modelName: string;
  code: string;
  inspectionsCount: number;
  sharePct: number;
  aiDeflectionPct: number;
  avgSlaSeconds: string;
}

export interface AuditedPartAnalyticsRow {
  sku: string;
  partName: string;
  modelName: string;
  isNaturalWear: boolean;
  occurrences: number;
  sharePct: number;
  totalAuditedBrl: string;
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
  rawEmbedUrl = signal<string | null>(null);
  isOfflineFallback = signal<boolean>(false);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  // Executive BI Studio View Mode & Regional Filter
  biViewMode = signal<'native' | 'looker'>('native');
  selectedYardFilter = signal<string>('ALL');

  readonly yardSnapshots: YardBiSnapshot[] = [
    {
      key: 'ALL',
      label: 'Todas as Unidades (SP)',
      totalInspections: 480,
      aiPrecisionPct: '93,4%',
      avgSlaSeconds: '4,2s',
      auditedRevenueBrl: 'R$ 14.820,00',
      naturalWearCount: 230,
      preExistingCount: 144,
      badUseCount: 106,
    },
    {
      key: 'SP-04',
      label: 'Butantã (SP-04)',
      totalInspections: 168,
      aiPrecisionPct: '94,1%',
      avgSlaSeconds: '3,9s',
      auditedRevenueBrl: 'R$ 5.240,00',
      naturalWearCount: 81,
      preExistingCount: 50,
      badUseCount: 37,
    },
    {
      key: 'SP-01',
      label: 'Guarulhos (SP-01)',
      totalInspections: 132,
      aiPrecisionPct: '93,0%',
      avgSlaSeconds: '4,3s',
      auditedRevenueBrl: 'R$ 4.180,00',
      naturalWearCount: 63,
      preExistingCount: 40,
      badUseCount: 29,
    },
    {
      key: 'SP-07',
      label: 'Interlagos (SP-07)',
      totalInspections: 104,
      aiPrecisionPct: '92,8%',
      avgSlaSeconds: '4,4s',
      auditedRevenueBrl: 'R$ 3.190,00',
      naturalWearCount: 50,
      preExistingCount: 31,
      badUseCount: 23,
    },
    {
      key: 'SP-02',
      label: 'Lapa (SP-02)',
      totalInspections: 76,
      aiPrecisionPct: '93,6%',
      avgSlaSeconds: '4,1s',
      auditedRevenueBrl: 'R$ 2.210,00',
      naturalWearCount: 36,
      preExistingCount: 23,
      badUseCount: 17,
    },
  ];

  activeBiSnapshot = computed<YardBiSnapshot>(() => {
    const key = this.selectedYardFilter();
    return this.yardSnapshots.find((y) => y.key === key) ?? this.yardSnapshots[0];
  });

  readonly dailyVolumeBars: DailyInspectionBar[] = [
    { day: 'Seg', directPct: 76, workshopPct: 24, totalCount: 68 },
    { day: 'Ter', directPct: 80, workshopPct: 20, totalCount: 64 },
    { day: 'Qua', directPct: 79, workshopPct: 21, totalCount: 72 },
    { day: 'Qui', directPct: 82, workshopPct: 18, totalCount: 81 },
    { day: 'Sex', directPct: 78, workshopPct: 22, totalCount: 79 },
    { day: 'Sáb', directPct: 84, workshopPct: 16, totalCount: 62 },
    { day: 'Dom', directPct: 86, workshopPct: 14, totalCount: 54 },
  ];

  readonly verdictBreakdown: VerdictBreakdownRow[] = [
    {
      label: 'Desgaste natural de uso (R$ 0,00)',
      subtitle: 'Pastilhas, cabos e relação cobertos pelo plano',
      percentage: 48,
      count: 230,
      barClass: 'bg-[#05944F]',
      dotColor: '#05944F',
    },
    {
      label: 'Pré-existente na retirada (R$ 0,00)',
      subtitle: 'Protegido pela janela de 48h ou 100 km',
      percentage: 30,
      count: 144,
      barClass: 'bg-[#141414]',
      dotColor: '#141414',
    },
    {
      label: 'Dano por queda ou colisão',
      subtitle: 'Orçado pela tabela oficial da Loja Mottu',
      percentage: 22,
      count: 106,
      barClass: 'bg-[#D97706]',
      dotColor: '#D97706',
    },
  ];

  readonly fleetModelBreakdown: FleetModelAnalyticsRow[] = [
    {
      modelName: 'Mottu Sport 110i',
      code: 'MOTTU_SPORT_110I',
      inspectionsCount: 192,
      sharePct: 40,
      aiDeflectionPct: 81.2,
      avgSlaSeconds: '3,8s',
    },
    {
      modelName: 'Honda Pop 110i',
      code: 'HONDA_POP_110I',
      inspectionsCount: 144,
      sharePct: 30,
      aiDeflectionPct: 79.8,
      avgSlaSeconds: '4,1s',
    },
    {
      modelName: 'TVS Raider 125',
      code: 'TVS_RAIDER_125',
      inspectionsCount: 86,
      sharePct: 18,
      aiDeflectionPct: 77.4,
      avgSlaSeconds: '4,5s',
    },
    {
      modelName: 'TVS Apache 160',
      code: 'TVS_APACHE_160',
      inspectionsCount: 58,
      sharePct: 12,
      aiDeflectionPct: 74.6,
      avgSlaSeconds: '4,8s',
    },
  ];

  readonly topAuditedParts: AuditedPartAnalyticsRow[] = [
    {
      sku: 'SKU-SPORT-002',
      partName: 'Lona de Freio Traseira Sport',
      modelName: 'Mottu Sport 110i',
      isNaturalWear: true,
      occurrences: 94,
      sharePct: 88,
      totalAuditedBrl: 'R$ 0,00 (Isento)',
    },
    {
      sku: 'SKU-POP-003',
      partName: 'Lona de Freio Traseira Pop',
      modelName: 'Honda Pop 110i',
      isNaturalWear: true,
      occurrences: 78,
      sharePct: 74,
      totalAuditedBrl: 'R$ 0,00 (Isento)',
    },
    {
      sku: 'SKU-APA-005',
      partName: 'Kit Relação Completa Apache',
      modelName: 'TVS Apache 160',
      isNaturalWear: true,
      occurrences: 58,
      sharePct: 55,
      totalAuditedBrl: 'R$ 0,00 (Isento)',
    },
    {
      sku: 'SKU-SPORT-001',
      partName: 'Manete de Freio Direito',
      modelName: 'Mottu Sport 110i',
      isNaturalWear: false,
      occurrences: 46,
      sharePct: 44,
      totalAuditedBrl: 'R$ 2.760,00',
    },
    {
      sku: 'SKU-POP-002',
      partName: 'Espelho Retrovisor Esquerdo',
      modelName: 'Honda Pop 110i',
      isNaturalWear: false,
      occurrences: 34,
      sharePct: 32,
      totalAuditedBrl: 'R$ 2.482,00',
    },
    {
      sku: 'SKU-RAI-002',
      partName: 'Espelho Retrovisor Raider',
      modelName: 'TVS Raider 125',
      isNaturalWear: false,
      occurrences: 26,
      sharePct: 25,
      totalAuditedBrl: 'R$ 2.106,00',
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
          this.rawEmbedUrl.set(url);
          this.safeEmbedUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url));
          this.isOfflineFallback.set(false);
        } else {
          this.rawEmbedUrl.set(null);
          this.safeEmbedUrl.set(null);
          this.isOfflineFallback.set(true);
        }
      },
      error: () => {
        this.errorMessage.set('Erro ao carregar indicadores executivos de inteligência operacional.');
        this.isLoading.set(false);
        this.isOfflineFallback.set(true);
      },
    });
  }

  setBiViewMode(mode: 'native' | 'looker'): void {
    this.biViewMode.set(mode);
  }

  setYardFilter(yardKey: string): void {
    this.selectedYardFilter.set(yardKey);
  }

  refresh(): void {
    this.loadSummary();
  }
}
