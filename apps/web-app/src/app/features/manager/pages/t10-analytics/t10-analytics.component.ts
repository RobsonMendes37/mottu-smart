import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { ManagerService } from '../../services/manager.service';
import {
  AnalyticsSummaryResponseDto,
  DailyInspectionBar,
  VerdictBreakdownRow,
  YardBiSnapshot,
  FleetModelAnalyticsRow,
  AuditedPartAnalyticsRow,
  OpsAdvisorResponseDto,
  OpsStockoutAlertDto,
  OpsDataExplorerResponseDto,
} from '../../models/manager.models';
import { DesktopShellComponent } from '../../../../shared/layout/desktop-shell/desktop-shell.component';

export type {
  DailyInspectionBar,
  VerdictBreakdownRow,
  YardBiSnapshot,
  FleetModelAnalyticsRow,
  AuditedPartAnalyticsRow,
  OpsAdvisorResponseDto,
  OpsStockoutAlertDto,
  OpsDataExplorerResponseDto,
};

const DEFAULT_AI_ADVISOR: OpsAdvisorResponseDto = {
  yardKey: 'ALL',
  yardLabel: 'Todas as Unidades (SP)',
  generatedAt: new Date().toISOString(),
  modelUsed: 'gemini-3.5-flash-lite',
  demandGrowthPct: 18.4,
  projectedNext7DaysInspections: 135,
  executiveHeadline:
    'Crescimento de 18,4% na demanda de vistorias com alerta de reposição imediata para peças de freio e guidão.',
  growthAnalysisText:
    'Identificada aceleração de revisões nas motos Mottu Sport 110i e Honda Pop 110i, recomendando reposição preventiva no almoxarifado para manter as baias com liberação em até 30 minutos.',
  stockoutAlerts: [
    {
      sku: 'SKU-SPORT-001',
      partName: 'Manete de Freio Direito',
      modelName: 'Mottu Sport 110i',
      currentStock: 4,
      minStock: 8,
      dailyBurnRate: 1.5,
      daysToStockout: 3,
      riskLevel: 'CRITICAL',
      suggestedRestockQty: 20,
    },
    {
      sku: 'SKU-POP-002',
      partName: 'Espelho Retrovisor Esquerdo',
      modelName: 'Honda Pop 110i',
      currentStock: 5,
      minStock: 6,
      dailyBurnRate: 1.0,
      daysToStockout: 5,
      riskLevel: 'WARNING',
      suggestedRestockQty: 15,
    },
    {
      sku: 'SKU-SPORT-002',
      partName: 'Lona de Freio Traseira Sport',
      modelName: 'Mottu Sport 110i',
      currentStock: 28,
      minStock: 12,
      dailyBurnRate: 2.1,
      daysToStockout: 13,
      riskLevel: 'HEALTHY',
      suggestedRestockQty: 0,
    },
  ],
  recommendedActions: [
    'Autorizar reposição imediata de +20 un. de Manete de Freio Direito (SKU-SPORT-001) para evitar parada de baia em 3 dias.',
    'Priorizar check-up preventivo de lonas de freio traseiro nas motos Mottu Sport 110i acima de 10.000 km.',
    'Reforçar bancada do turno da manhã na unidade Butantã para absorver a projeção de 135 vistorias nos próximos 7 dias.',
  ],
};

@Component({
  selector: 'app-t10-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DesktopShellComponent],
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

  // AI BigQuery Ops Advisor Signals
  aiAdvisor = signal<OpsAdvisorResponseDto>(DEFAULT_AI_ADVISOR);
  aiAdvisorLoading = signal<boolean>(false);
  aiAdvisorSuccessBanner = signal<string | null>(null);

  // Google-Grade Conversational AI Data Explorer Signals
  searchQuery = signal<string>('Quais peças estão com estoque crítico?');
  dataExplorerLoading = signal<boolean>(false);
  dataExplorerResult = signal<OpsDataExplorerResponseDto | null>({
    query: 'Quais peças estão com estoque crítico?',
    yardKey: 'ALL',
    yardLabel: 'Todas as Unidades (SP)',
    generatedAt: new Date().toISOString(),
    modelUsed: 'gemini-3.5-flash-lite',
    directAnswer: 'Foram identificados 5 itens com estoque crítico ou abaixo da margem de segurança no galpão.',
    keyMetricLabel: 'Itens em Ruptura Iminente',
    keyMetricValue: '5 SKUs Críticos',
    keyMetricTrend: 'Ação de compra imediata recomendada',
    columns: ['SKU', 'Componente e Modelo', 'Saldo em Almoxarifado', 'Consumo Médio', 'Previsão p/ Ruptura', 'Lote Sugerido'],
    rows: [
      ['SKU-SPORT-001', 'Manete de Freio Direito (Mottu Sport 110i)', '4 un. (mín 8)', '1.5 un./dia', '3 dias (CRÍTICO)', '+20 un.'],
      ['SKU-POP-002', 'Espelho Retrovisor Par (Honda Pop 110i)', '4 un. (mín 8)', '1.2 un./dia', '3 dias (CRÍTICO)', '+20 un.'],
      ['SKU-RAI-002', 'Pedaleira Dianteira Direita (TVS Raider 125)', '4 un. (mín 8)', '1.1 un./dia', '3 dias (CRÍTICO)', '+20 un.'],
      ['SKU-SPORT-002', 'Lona de Freio Traseira (Mottu Sport 110i)', '6 un. (mín 8)', '1.4 un./dia', '4 dias (CRÍTICO)', '+18 un.'],
      ['SKU-POP-003', 'Cabo de Embreagem (Honda Pop 110i)', '6 un. (mín 8)', '1.3 un./dia', '4 dias (CRÍTICO)', '+18 un.']
    ],
    suggestedActionLabel: 'Repor Peças no Almoxarifado WMS',
    suggestedActionRoute: '/mechanic/operations'
  });
  copySummaryFeedback = signal<boolean>(false);

  readonly quickPrompts = [
    { label: 'Peças críticas', query: 'Quais peças estão com estoque crítico e precisam de reposição?' },
    { label: 'Vistorias pendentes', query: 'Qual a situação das vistorias e laudos de hoje?' },
    { label: 'Modelos com mais trocas', query: 'Qual modelo de moto concentra mais revisões e trocas de peças?' },
    { label: 'Comparar unidades', query: 'Comparativo de desempenho entre Butantã, Guarulhos, Interlagos e Lapa' },
    { label: 'Contratos e Pix', query: 'Qual a situação atual dos contratos e pagamentos Pix?' },
    { label: 'Resumo geral', query: 'Resumo operacional consolidado da operação de hoje' },
  ];

  private readonly initialYardSnapshots: YardBiSnapshot[] = [
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

  yardSnapshotsSignal = signal<YardBiSnapshot[]>(this.initialYardSnapshots);
  get yardSnapshots(): YardBiSnapshot[] {
    return this.yardSnapshotsSignal();
  }

  activeBiSnapshot = computed<YardBiSnapshot>(() => {
    const key = this.selectedYardFilter();
    return this.yardSnapshotsSignal().find((y) => y.key === key) ?? this.yardSnapshotsSignal()[0];
  });

  private readonly initialDailyVolumeBars: DailyInspectionBar[] = [
    { day: 'Seg', directPct: 76, workshopPct: 24, totalCount: 68 },
    { day: 'Ter', directPct: 80, workshopPct: 20, totalCount: 64 },
    { day: 'Qua', directPct: 79, workshopPct: 21, totalCount: 72 },
    { day: 'Qui', directPct: 82, workshopPct: 18, totalCount: 81 },
    { day: 'Sex', directPct: 78, workshopPct: 22, totalCount: 79 },
    { day: 'Sáb', directPct: 84, workshopPct: 16, totalCount: 62 },
    { day: 'Dom', directPct: 86, workshopPct: 14, totalCount: 54 },
  ];

  dailyVolumeBarsSignal = signal<DailyInspectionBar[]>(this.initialDailyVolumeBars);
  get dailyVolumeBars(): DailyInspectionBar[] {
    return this.dailyVolumeBarsSignal();
  }

  private readonly initialVerdictBreakdown: VerdictBreakdownRow[] = [
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

  verdictBreakdownSignal = signal<VerdictBreakdownRow[]>(this.initialVerdictBreakdown);
  get verdictBreakdown(): VerdictBreakdownRow[] {
    return this.verdictBreakdownSignal();
  }

  private readonly initialFleetModelBreakdown: FleetModelAnalyticsRow[] = [
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

  fleetModelBreakdownSignal = signal<FleetModelAnalyticsRow[]>(this.initialFleetModelBreakdown);
  get fleetModelBreakdown(): FleetModelAnalyticsRow[] {
    return this.fleetModelBreakdownSignal();
  }

  private readonly initialTopAuditedParts: AuditedPartAnalyticsRow[] = [
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

  topAuditedPartsSignal = signal<AuditedPartAnalyticsRow[]>(this.initialTopAuditedParts);
  get topAuditedParts(): AuditedPartAnalyticsRow[] {
    return this.topAuditedPartsSignal();
  }

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
    this.loadDetailedAnalytics();
    this.loadAiAdvisor();
  }

  loadSummary(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.managerService?.getAnalyticsSummary?.()?.subscribe?.({
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

  loadDetailedAnalytics(): void {
    if (!this.managerService?.getDetailedAnalytics) return;

    this.managerService.getDetailedAnalytics().subscribe({
      next: (data) => {
        if (data.yardSnapshots && data.yardSnapshots.length > 0) {
          this.yardSnapshotsSignal.set(data.yardSnapshots);
        }
        if (data.dailyVolumeBars && data.dailyVolumeBars.length > 0) {
          this.dailyVolumeBarsSignal.set(data.dailyVolumeBars);
        }
        if (data.verdictBreakdown && data.verdictBreakdown.length > 0) {
          this.verdictBreakdownSignal.set(data.verdictBreakdown);
        }
        if (data.fleetModelBreakdown && data.fleetModelBreakdown.length > 0) {
          this.fleetModelBreakdownSignal.set(data.fleetModelBreakdown);
        }
        if (data.topAuditedParts && data.topAuditedParts.length > 0) {
          this.topAuditedPartsSignal.set(data.topAuditedParts);
        }
      },
      error: () => {},
    });
  }

  loadAiAdvisor(): void {
    if (!this.managerService?.getAiAdvisorAdvice) return;
    this.aiAdvisorLoading.set(true);

    this.managerService.getAiAdvisorAdvice(this.selectedYardFilter()).subscribe({
      next: (res) => {
        if (res) this.aiAdvisor.set(res);
        this.aiAdvisorLoading.set(false);
      },
      error: () => {
        this.aiAdvisorLoading.set(false);
      },
    });
  }

  restockPart(sku: string, suggestedQty: number): void {
    const delta = suggestedQty > 0 ? suggestedQty : 10;

    // Atualização otimista no painel de alertas de estoque
    this.aiAdvisor.update((adv) => {
      if (!adv) return adv;
      return {
        ...adv,
        stockoutAlerts: adv.stockoutAlerts.map((a) =>
          a.sku === sku
            ? {
                ...a,
                currentStock: a.currentStock + delta,
                riskLevel: 'HEALTHY' as const,
                daysToStockout: 21,
                suggestedRestockQty: 0,
              }
            : a
        ),
      };
    });

    this.aiAdvisorSuccessBanner.set(
      `Lote de +${delta} un. reposto com sucesso no almoxarifado para a peça ${sku}!`
    );

    if (this.managerService?.restockPart) {
      this.managerService.restockPart(sku, delta).subscribe({
        next: () => {},
        error: () => {},
      });
    }
  }

  setBiViewMode(mode: 'native' | 'looker'): void {
    this.biViewMode.set(mode);
  }

  scrollToSection(sectionId: string): void {
    if (typeof document !== 'undefined') {
      const el = document.getElementById(sectionId);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }
  }

  setYardFilter(yardKey: string): void {
    this.selectedYardFilter.set(yardKey);
    this.loadAiAdvisor();
  }

  executeDataQuery(queryText?: string): void {
    const q = (queryText ?? this.searchQuery()).trim();
    if (!q) return;
    this.searchQuery.set(q);
    this.dataExplorerLoading.set(true);

    if (!this.managerService?.askDataExplorer) {
      this.dataExplorerLoading.set(false);
      return;
    }

    this.managerService.askDataExplorer(q, this.selectedYardFilter()).subscribe({
      next: (res) => {
        if (res) this.dataExplorerResult.set(res);
        this.dataExplorerLoading.set(false);
      },
      error: () => {
        this.dataExplorerLoading.set(false);
      },
    });
  }

  copyExecutiveSummary(): void {
    const res = this.dataExplorerResult();
    if (!res) return;

    const summaryText = `*MOTTU SMART OPS — RELATÓRIO OPERACIONAL*\n` +
      `• *Consulta:* ${res.query}\n` +
      `• *Unidade:* ${res.yardLabel}\n` +
      `• *Diagnóstico:* ${res.directAnswer}\n` +
      `• *Métrica:* ${res.keyMetricLabel}: ${res.keyMetricValue} (${res.keyMetricTrend})\n\n` +
      `*Evidências:*\n` +
      res.columns.join(' | ') + '\n' +
      res.rows.map((r) => r.join(' | ')).join('\n');

    if (typeof navigator !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(summaryText).then(() => {
        this.copySummaryFeedback.set(true);
        setTimeout(() => this.copySummaryFeedback.set(false), 2500);
      });
    }
  }

  exportCsv(): void {
    const res = this.dataExplorerResult();
    if (!res || typeof window === 'undefined') return;

    const csvContent =
      'data:text/csv;charset=utf-8,' +
      [res.columns.join(','), ...res.rows.map((e) => e.map((cell) => `"${cell.replace(/"/g, '""')}"`).join(','))].join(
        '\n'
      );

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `relatorio_operacional_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  refresh(): void {
    this.loadSummary();
    this.loadDetailedAnalytics();
    this.loadAiAdvisor();
    this.executeDataQuery();
  }
}
