import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
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
    'Crescimento de 18,4% na demanda de vistorias com alerta de reposição preventiva de componentes nos galpões.',
  growthAnalysisText:
    'Identificada aceleração de revisões nas motos Mottu Sport 110i e Honda Pop 110i, recomendando reposição preventiva no almoxarifado para manter as baias com liberação em até 30 minutos.',
  horizonDays: 7,
  scenario: 'NORMAL',
  scenarioLabel: 'Operação Padrão',
  systemCategory: 'ALL',
  totalRestockBudgetBrl: 8614.00,
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
      unitPriceBrl: 25.00,
      estimatedCostBrl: 500.00,
      projectedDemand: 11,
      systemCategory: 'BRAKE_HANDLEBAR',
    },
    {
      sku: 'SKU-SPORT-002',
      partName: 'Lona de Freio Traseira Sport',
      modelName: 'Mottu Sport 110i',
      currentStock: 6,
      minStock: 8,
      dailyBurnRate: 1.4,
      daysToStockout: 4,
      riskLevel: 'CRITICAL',
      suggestedRestockQty: 18,
      unitPriceBrl: 33.00,
      estimatedCostBrl: 594.00,
      projectedDemand: 10,
      systemCategory: 'BRAKE_HANDLEBAR',
    },
    {
      sku: 'SKU-PNEU-TRAS-SPORT',
      partName: 'Pneu Traseiro 80/100-14',
      modelName: 'Mottu Sport 110i',
      currentStock: 5,
      minStock: 8,
      dailyBurnRate: 1.2,
      daysToStockout: 4,
      riskLevel: 'CRITICAL',
      suggestedRestockQty: 15,
      unitPriceBrl: 180.00,
      estimatedCostBrl: 2700.00,
      projectedDemand: 9,
      systemCategory: 'TIRES_WHEELS',
    },
    {
      sku: 'SKU-RAI-004',
      partName: 'Pneu Dianteiro 90/90-17',
      modelName: 'TVS Raider 125',
      currentStock: 6,
      minStock: 8,
      dailyBurnRate: 1.0,
      daysToStockout: 6,
      riskLevel: 'WARNING',
      suggestedRestockQty: 14,
      unitPriceBrl: 195.00,
      estimatedCostBrl: 2730.00,
      projectedDemand: 7,
      systemCategory: 'TIRES_WHEELS',
    },
    {
      sku: 'SKU-POP-003',
      partName: 'Cabo de Embreagem',
      modelName: 'Honda Pop 110i',
      currentStock: 4,
      minStock: 8,
      dailyBurnRate: 1.3,
      daysToStockout: 3,
      riskLevel: 'CRITICAL',
      suggestedRestockQty: 20,
      unitPriceBrl: 28.00,
      estimatedCostBrl: 560.00,
      projectedDemand: 9,
      systemCategory: 'ENGINE_TRANSMISSION',
    },
    {
      sku: 'SKU-SPT-006',
      partName: 'Pedal de Câmbio Sport',
      modelName: 'Mottu Sport 110i',
      currentStock: 5,
      minStock: 8,
      dailyBurnRate: 1.1,
      daysToStockout: 4,
      riskLevel: 'CRITICAL',
      suggestedRestockQty: 19,
      unitPriceBrl: 38.00,
      estimatedCostBrl: 722.00,
      projectedDemand: 8,
      systemCategory: 'ENGINE_TRANSMISSION',
    },
    {
      sku: 'SKU-POP-002',
      partName: 'Espelho Retrovisor Par',
      modelName: 'Honda Pop 110i',
      currentStock: 4,
      minStock: 8,
      dailyBurnRate: 1.0,
      daysToStockout: 4,
      riskLevel: 'CRITICAL',
      suggestedRestockQty: 18,
      unitPriceBrl: 39.00,
      estimatedCostBrl: 702.00,
      projectedDemand: 7,
      systemCategory: 'FAIRING_BODY',
    },
    {
      sku: 'SKU-RAI-002',
      partName: 'Pedaleira Dianteira Direita',
      modelName: 'TVS Raider 125',
      currentStock: 4,
      minStock: 8,
      dailyBurnRate: 0.9,
      daysToStockout: 4,
      riskLevel: 'CRITICAL',
      suggestedRestockQty: 18,
      unitPriceBrl: 46.00,
      estimatedCostBrl: 828.00,
      projectedDemand: 6,
      systemCategory: 'FAIRING_BODY',
    },
  ],
  recommendedActions: [
    'Autorizar reposição imediata de peças em risco crítico para evitar parada de baia nos próximos dias.',
    'Priorizar check-up preventivo de lonas e pneus nas motos Mottu Sport 110i acima de 10.000 km.',
    'Reforçar bancada de mecânicos para absorver a projeção de 135 vistorias nos próximos 7 dias.',
  ],
};

@Component({
  selector: 'app-t10-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DesktopShellComponent],
  templateUrl: './t10-analytics.component.html',
})
export class T10AnalyticsComponent implements OnInit, OnDestroy {
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

  // Predictive Forecasting Parameters & Scenario Simulation (Google-Grade What-If)
  selectedHorizon = signal<number>(7);
  selectedSystemCategory = signal<string>('ALL');
  selectedScenario = signal<string>('NORMAL');

  readonly horizonOptions = [
    { value: 7, label: '7 dias' },
    { value: 15, label: '15 dias' },
    { value: 30, label: '30 dias' },
    { value: 90, label: '90 dias' },
  ];

  readonly systemCategoryOptions = [
    { value: 'ALL', label: 'Todos os Sistemas' },
    { value: 'BRAKE_HANDLEBAR', label: 'Freio e Guidão' },
    { value: 'TIRES_WHEELS', label: 'Pneus e Rodas' },
    { value: 'ENGINE_TRANSMISSION', label: 'Motor e Câmbio' },
    { value: 'FAIRING_BODY', label: 'Carenagem e Chassi' },
  ];

  readonly scenarioOptions = [
    {
      value: 'NORMAL',
      label: 'Operação Padrão',
      badge: '1.0x Nominal',
      desc: 'Histórico nominal sem intempéries climáticas',
    },
    {
      value: 'RAIN_PEAK',
      label: 'Pico de Chuva e Quedas',
      badge: '+35% Estresse',
      desc: 'Chuva intensa em SP: quedas e quebra de manetes, espelhos e pastilhas',
    },
    {
      value: 'FLEET_EXPANSION',
      label: 'Expansão de Frota',
      badge: '+25% Giro',
      desc: 'Novos contratos ativos e maior quilometragem rodada por dia',
    },
  ];

  displayedStockoutAlerts = computed(() => {
    const alerts = this.aiAdvisor()?.stockoutAlerts || [];
    const cat = this.selectedSystemCategory();
    if (!cat || cat === 'ALL') return alerts;
    return alerts.filter((a) => (a.systemCategory || '').toUpperCase() === cat.toUpperCase());
  });

  totalStockoutBudget = computed(() => {
    const alerts = this.displayedStockoutAlerts();
    return alerts.reduce(
      (sum, a) => sum + (a.estimatedCostBrl || (a.suggestedRestockQty * (a.unitPriceBrl || 25))),
      0
    );
  });

  criticalAlertsCount = computed(() => {
    return this.displayedStockoutAlerts().filter((a) => a.riskLevel === 'CRITICAL').length;
  });

  getSelectedSystemLabel(): string {
    const cat = this.selectedSystemCategory();
    const opt = this.systemCategoryOptions.find((o) => o.value === cat);
    return opt ? opt.label : 'Todos os Sistemas';
  }

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
    this.initVoices();
    this.initSpeechRecognition();
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
          const embedUrl = url.includes('/embed/') ? url : url.replace('/reporting/', '/embed/reporting/');
          this.rawEmbedUrl.set(url);
          this.safeEmbedUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl));
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

  // =========================================================================
  // MONALISA • ASSISTENTE OPERACIONAL DE PÁTIO (VOICE & SKILLS AGENT)
  // =========================================================================
  readonly isCopilotOpen = signal<boolean>(false);
  readonly copilotInput = signal<string>('');
  readonly copilotLoading = signal<boolean>(false);
  readonly isSpeaking = signal<boolean>(false);
  readonly speakingMessageId = signal<number | null>(null);
  readonly autoVoiceEnabled = signal<boolean>(true);
  readonly isAiOfflineMode = signal<boolean>(false);

  // Reconhecimento de Voz (Microfone do Usuário)
  readonly isListening = signal<boolean>(false);
  readonly speechRecognitionSupported = signal<boolean>(false);
  readonly speechNotice = signal<string | null>(null);
  private recognition: any = null;
  private availableVoices: any[] = [];
  private currentAudioPlayer: HTMLAudioElement | null = null;
  private currentAudioUrl: string | null = null;
  private currentTtsSub: Subscription | null = null;
  private aiAdvisorSub: Subscription | null = null;
  private currentSpeechSessionId = 0;
  private typewriterInterval: any = null;

  readonly operationalSkills = [
    { id: 'SKILL_ESTOQUE', label: 'Estoque Crítico' },
    { id: 'SKILL_FROTA', label: 'Status da Frota' },
    { id: 'SKILL_OFICINA', label: 'Baias da Oficina' },
    { id: 'SKILL_VISTORIAS', label: 'Vistorias & Deflexão' },
    { id: 'SKILL_FINANCEIRO', label: 'Financeiro & Avarias' },
    { id: 'SKILL_APROVACOES', label: 'Aprovações Pendentes' },
    { id: 'SKILL_BUSCA_PLACA', label: 'Consultar Placa / OS' },
  ];

  readonly copilotMessages = signal<Array<{
    id: number;
    sender: 'user' | 'assistant';
    text: string;
    displayedText?: string;
    spokenText?: string;
    shouldShowTable?: boolean;
    time: string;
    metricLabel?: string;
    metricValue?: string;
    columns?: string[];
    rows?: string[][];
  }>>([
    {
      id: 1,
      sender: 'assistant',
      text: 'Olá! Sou a Melisa, sua assistente operacional da Mottu. Pode me perguntar sobre a frota, escolher uma das Skills abaixo ou falar no microfone.',
      displayedText: 'Olá! Sou a Melisa, sua assistente operacional da Mottu. Pode me perguntar sobre a frota, escolher uma das Skills abaixo ou falar no microfone.',
      spokenText: 'Olá! Sou a Melisa. Em que posso apoiar na operação?',
      shouldShowTable: false,
      time: 'Agora',
    },
  ]);

  readonly copilotSuggestions = [
    'Qual modelo tem maior índice de avarias?',
    'Há risco de falta de peças no Butantã?',
    'Resumo do crescimento de demanda no mês',
    'Qual proporção de vistorias resolvida sem fila?',
  ];

  executeSkill(skillId: string, skillLabel: string): void {
    this.sendCopilotMessage(skillLabel, skillId);
  }

  initVoices(): void {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) return;
    const updateVoices = () => {
      try {
        const voices = window.speechSynthesis.getVoices();
        if (voices && voices.length > 0) {
          this.availableVoices = voices;
        }
      } catch {
        // Safe SSR / JSDOM guard
      }
    };
    updateVoices();
    try {
      window.speechSynthesis.onvoiceschanged = updateVoices;
    } catch {
      // Ignore
    }
  }

  initSpeechRecognition(): void {
    if (typeof window === 'undefined') return;
    try {
      const SpeechRec = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
      if (SpeechRec) {
        this.speechRecognitionSupported.set(true);
        this.recognition = new SpeechRec();
        this.recognition.lang = 'pt-BR';
        this.recognition.continuous = false;
        this.recognition.interimResults = true;

        this.recognition.onstart = () => {
          this.isListening.set(true);
          this.speechNotice.set(null);
        };

        this.recognition.onresult = (event: any) => {
          let transcript = '';
          for (let i = event.resultIndex; i < event.results.length; ++i) {
            transcript += event.results[i][0].transcript;
          }
          if (transcript) {
            this.copilotInput.set(transcript);
          }
        };

        this.recognition.onerror = (event: any) => {
          this.isListening.set(false);
          const err = event?.error;
          if (err === 'not-allowed' || err === 'service-not-allowed') {
            this.speechNotice.set('Permissão de microfone negada. Clique no ícone de cadeado/configurações ao lado da barra de endereço para permitir o microfone.');
          } else if (err === 'no-speech') {
            this.speechNotice.set('Nenhuma fala foi detectada. Tente falar novamente mais próximo ao microfone.');
          } else if (err === 'audio-capture') {
            this.speechNotice.set('Nenhum microfone encontrado neste dispositivo.');
          } else if (err === 'network') {
            this.speechNotice.set('Falha de rede no serviço de voz do navegador.');
          }
        };

        this.recognition.onend = () => {
          this.isListening.set(false);
          const spoken = this.copilotInput().trim();
          if (spoken) {
            this.sendCopilotMessage();
          }
        };
      } else {
        this.speechRecognitionSupported.set(false);
      }
    } catch {
      this.speechRecognitionSupported.set(false);
    }
  }

  dismissSpeechNotice(): void {
    this.speechNotice.set(null);
  }

  cancelSpeechRecognition(): void {
    if (this.recognition) {
      try {
        this.recognition.stop();
      } catch {}
    }
    this.isListening.set(false);
  }

  finishAndSendSpeech(): void {
    if (this.recognition) {
      try {
        this.recognition.stop();
      } catch {}
    }
    this.isListening.set(false);
    const spoken = this.copilotInput().trim();
    if (spoken) {
      this.sendCopilotMessage();
    }
  }

  toggleSpeechRecognition(): void {
    if (typeof window === 'undefined') return;

    if (!this.speechRecognitionSupported() || !this.recognition) {
      const isSecure = typeof window !== 'undefined' && (window.isSecureContext || window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1');
      if (!isSecure) {
        this.speechNotice.set('O microfone do navegador exige conexão segura HTTPS ou localhost. Em conexões HTTP por IP (como 161.153.2.148), os navegadores desabilitam o áudio por segurança. Acesse via HTTPS, localhost ou digite sua pergunta.');
      } else {
        this.speechNotice.set('Reconhecimento de fala nativo não suportado neste navegador. Recomendamos utilizar o Google Chrome ou Microsoft Edge.');
      }
      return;
    }

    if (this.isListening()) {
      this.cancelSpeechRecognition();
    } else {
      this.stopSpeech();
      this.speechNotice.set(null);
      this.copilotInput.set('');

      try {
        this.recognition.start();
      } catch (err: any) {
        this.isListening.set(false);
        this.speechNotice.set('Não foi possível ativar o microfone: ' + (err?.message || 'Permissão necessária.'));
      }
    }
  }

  toggleAutoVoice(): void {
    const next = !this.autoVoiceEnabled();
    this.autoVoiceEnabled.set(next);
    if (!next) {
      this.stopSpeech();
    }
  }

  toggleCopilot(): void {
    const nextState = !this.isCopilotOpen();
    this.isCopilotOpen.set(nextState);
    if (!nextState) {
      this.stopSpeech();
      if (this.isListening() && this.recognition) {
        try {
          this.recognition.stop();
        } catch {}
        this.isListening.set(false);
      }
    }
  }

  sendCopilotMessage(customPrompt?: string, skillId?: string): void {
    const prompt = (customPrompt ?? this.copilotInput()).trim();
    if (!prompt) return;

    // Cancela imediatamente qualquer áudio, requisição ou animação anterior para evitar sobreposição de vozes
    this.stopSpeech();

    const userMsgId = Date.now();
    const nowStr = new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

    // Append user message
    this.copilotMessages.update((msgs) => [
      ...msgs,
      { id: userMsgId, sender: 'user', text: prompt, time: nowStr },
    ]);
    this.copilotInput.set('');
    this.copilotLoading.set(true);

    // Context memory: take last 6 messages
    const history = this.copilotMessages().slice(-6).map((m) => ({
      sender: m.sender,
      text: m.text,
    }));

    if (this.managerService?.askDataExplorer) {
      this.managerService.askDataExplorer(prompt, this.selectedYardFilter(), skillId, history).subscribe({
        next: (res) => {
          this.copilotLoading.set(false);
          const assistantMsgId = Date.now() + 1;
          const answer = res.directAnswer || 'Análise operacional concluída com sucesso com os dados de pátio e estoque.';
          const spoken = res.spokenAnswer || answer;
          const shouldShowTable = res.shouldShowTable !== false;

          if (res.isAiOffline !== undefined) {
            this.isAiOfflineMode.set(res.isAiOffline);
          }

          // Mensagem com displayedText inicial para garantir visibilidade imediata com animação progressiva
          this.copilotMessages.update((msgs) => [
            ...msgs,
            {
              id: assistantMsgId,
              sender: 'assistant',
              text: answer,
              displayedText: answer,
              spokenText: spoken,
              shouldShowTable,
              time: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
              metricLabel: res.keyMetricLabel,
              metricValue: res.keyMetricValue,
              columns: res.columns,
              rows: res.rows,
            },
          ]);

          // Efeito de digitação suave em 0.5s (500ms)
          let typewriterFinished = false;
          this.animateTypewriter(assistantMsgId, answer, () => {
            typewriterFinished = true;
          });

          // Disparo sincronizado da voz ultra-curta e executiva (15 palavras / 2 a 3 segundos)
          if (this.autoVoiceEnabled()) {
            this.speakText(spoken, assistantMsgId, () => typewriterFinished);
          }
        },
        error: () => {
          this.copilotLoading.set(false);
          this.isAiOfflineMode.set(true);
          const assistantMsgId = Date.now() + 1;
          const fallbackAnswer = `Com base nos dados consolidados da unidade ${this.selectedYardFilter()}: temos ${this.activeBiSnapshot().totalInspections} vistorias analisadas, com ${this.activeBiSnapshot().aiPrecisionPct} de resoluções diretas e projeção de ${this.aiAdvisor().projectedNext7DaysInspections} atendimentos para os próximos ${this.selectedHorizon()} dias.`;
          const fallbackSpoken = `Operação da unidade ${this.selectedYardFilter()} com ${this.activeBiSnapshot().totalInspections} vistorias analisadas.`;

          this.copilotMessages.update((msgs) => [
            ...msgs,
            {
              id: assistantMsgId,
              sender: 'assistant',
              text: fallbackAnswer,
              displayedText: fallbackAnswer,
              spokenText: fallbackSpoken,
              shouldShowTable: false,
              time: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
            },
          ]);

          let typewriterFinished = false;
          this.animateTypewriter(assistantMsgId, fallbackAnswer, () => {
            typewriterFinished = true;
          });

          if (this.autoVoiceEnabled()) {
            this.speakText(fallbackSpoken, assistantMsgId, () => typewriterFinished);
          }
        },
      });
    } else {
      this.copilotLoading.set(false);
    }
  }

  animateTypewriter(messageId: number, fullText: string, onComplete?: () => void): void {
    if (this.typewriterInterval) {
      clearInterval(this.typewriterInterval);
      this.typewriterInterval = null;
    }

    const totalDurationMs = 500; // 0.5s conforme solicitado
    const intervalMs = 25;
    const totalSteps = Math.max(1, Math.floor(totalDurationMs / intervalMs));
    let currentStep = 1;

    const initialChars = Math.max(1, Math.ceil(fullText.length / totalSteps));
    this.copilotMessages.update((msgs) =>
      msgs.map((m) => (m.id === messageId ? { ...m, displayedText: fullText.slice(0, initialChars) } : m))
    );

    this.typewriterInterval = setInterval(() => {
      currentStep++;
      const progress = currentStep / totalSteps;
      const charCount = Math.min(fullText.length, Math.ceil(progress * fullText.length));
      const partial = fullText.slice(0, charCount);

      this.copilotMessages.update((msgs) =>
        msgs.map((m) => (m.id === messageId ? { ...m, displayedText: partial } : m))
      );

      if (currentStep >= totalSteps) {
        if (this.typewriterInterval) {
          clearInterval(this.typewriterInterval);
          this.typewriterInterval = null;
        }
        this.copilotMessages.update((msgs) =>
          msgs.map((m) => (m.id === messageId ? { ...m, displayedText: fullText } : m))
        );
        if (onComplete) onComplete();
      }
    }, intervalMs);
  }

  cleanTextForSpeech(text: string): string {
    if (!text) return '';
    return text
      .replace(/R\$\s*(\d+)[,.](\d{2})/g, '$1 reais e $2 centavos')
      .replace(/R\$\s*(\d+)/g, '$1 reais')
      .replace(/(\d+)[,.](\d+)%/g, '$1 vírgula $2 por cento')
      .replace(/(\d+)%/g, '$1 por cento')
      .replace(/\bkm\/h\b/gi, 'quilômetros por hora')
      .replace(/\bkm\b/gi, 'quilômetros')
      .replace(/\bun\.\b/gi, 'unidades')
      .replace(/\bSP-04\b/gi, 'unidade Butantã')
      .replace(/\bSP-01\b/gi, 'unidade Guarulhos')
      .replace(/\bSP-03\b/gi, 'unidade Interlagos')
      .replace(/\bSP-02\b/gi, 'unidade Lapa')
      .replace(/\bTMA\b/g, 'tempo médio de atendimento')
      .replace(/\bVTEX\b/gi, 'Loja Mottu')
      .replace(/\bSKU\b/gi, 'código de peça')
      .replace(/[*#_`~]/g, '')
      .replace(/https?:\/\/\S+/g, '')
      .replace(/[\(\)\[\]]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
  }

  getBestPtVoice(): any {
    if (!this.availableVoices || this.availableVoices.length === 0) {
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        try {
          this.availableVoices = window.speechSynthesis.getVoices() || [];
        } catch {
          return null;
        }
      }
    }
    const ptVoices = this.availableVoices.filter((v: any) => v.lang && v.lang.toLowerCase().startsWith('pt'));
    if (ptVoices.length === 0) return null;

    // 1. Voz Oficial do Google (Google português do Brasil)
    const googleVoice = ptVoices.find((v: any) =>
      v.name.toLowerCase().includes('google') && (v.lang.includes('BR') || v.lang.includes('pt'))
    );
    if (googleVoice) return googleVoice;

    // 2. Microsoft Thalita Online (Natural)
    const thalitaVoice = ptVoices.find((v: any) =>
      v.name.toLowerCase().includes('thalita') && v.lang.includes('BR')
    );
    if (thalitaVoice) return thalitaVoice;

    // 3. Vozes Naturais / Neurais pt-BR
    const naturalVoice = ptVoices.find((v: any) =>
      (v.name.includes('Natural') || v.name.includes('Neural') || v.name.includes('Online')) && v.lang.includes('BR')
    );
    if (naturalVoice) return naturalVoice;

    // 4. Voz feminina pt-BR para Melisa
    const femaleVoice = ptVoices.find((v: any) =>
      v.name.includes('Francisca') ||
      v.name.includes('Maria') ||
      v.name.includes('Luciana') ||
      v.name.includes('Yara') ||
      v.name.includes('Heloisa') ||
      v.name.includes('Leticia')
    );
    if (femaleVoice) return femaleVoice;

    const ptBrVoice = ptVoices.find((v: any) => v.lang === 'pt-BR');
    if (ptBrVoice) return ptBrVoice;

    return ptVoices[0];
  }

  speakText(text: string, msgId: number, canPlayCheck?: () => boolean): void {
    if (typeof window === 'undefined') return;

    this.stopSpeech(true);
    const sessionId = ++this.currentSpeechSessionId;

    const spokenText = this.cleanTextForSpeech(text);
    if (!spokenText) return;

    // 1. Tentar áudio neural gerado no servidor (Thalita Multilingual Neural pt-BR)
    if (this.managerService?.generateSpeechAudio) {
      this.currentTtsSub = this.managerService.generateSpeechAudio(spokenText).subscribe({
        next: (blob) => {
          // Descarta se a sessão de voz foi cancelada ou substituída por outra mensagem
          if (sessionId !== this.currentSpeechSessionId) return;

          if (blob && blob.size > 200) {
            try {
              if (this.currentAudioUrl) {
                try {
                  URL.revokeObjectURL(this.currentAudioUrl);
                } catch {}
              }
              this.currentAudioUrl = URL.createObjectURL(blob);
              const player = new Audio(this.currentAudioUrl);
              this.currentAudioPlayer = player;

              const triggerPlayback = () => {
                if (sessionId !== this.currentSpeechSessionId) return;
                this.isSpeaking.set(true);
                this.speakingMessageId.set(msgId);
                player.play().catch(() => {
                  if (sessionId === this.currentSpeechSessionId) {
                    this.speakWithWebSpeech(spokenText, msgId, sessionId);
                  }
                });
              };

              player.onended = () => {
                if (sessionId === this.currentSpeechSessionId) {
                  this.isSpeaking.set(false);
                  this.speakingMessageId.set(null);
                }
              };
              player.onerror = () => {
                if (sessionId === this.currentSpeechSessionId) {
                  this.speakWithWebSpeech(spokenText, msgId, sessionId);
                }
              };

              // Se a digitação (0.5s) ainda estiver acontecendo, aguarda para iniciar parelho
              if (canPlayCheck && !canPlayCheck()) {
                const waitCheck = setInterval(() => {
                  if (sessionId !== this.currentSpeechSessionId) {
                    clearInterval(waitCheck);
                    return;
                  }
                  if (canPlayCheck()) {
                    clearInterval(waitCheck);
                    triggerPlayback();
                  }
                }, 40);
                setTimeout(() => {
                  clearInterval(waitCheck);
                  if (sessionId === this.currentSpeechSessionId && !this.isSpeaking()) {
                    triggerPlayback();
                  }
                }, 520);
              } else {
                triggerPlayback();
              }
              return;
            } catch {
              this.speakWithWebSpeech(spokenText, msgId, sessionId);
              return;
            }
          }
          this.speakWithWebSpeech(spokenText, msgId, sessionId);
        },
        error: () => {
          if (sessionId === this.currentSpeechSessionId) {
            this.speakWithWebSpeech(spokenText, msgId, sessionId);
          }
        },
      });
      return;
    }

    this.speakWithWebSpeech(spokenText, msgId, sessionId);
  }

  speakWithWebSpeech(spokenText: string, msgId: number, sessionId: number): void {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) return;
    if (sessionId !== this.currentSpeechSessionId) return;

    try {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(spokenText);
      utterance.lang = 'pt-BR';
      utterance.rate = 1.0;
      utterance.pitch = 1.0;

      const bestVoice = this.getBestPtVoice();
      if (bestVoice) {
        utterance.voice = bestVoice;
      }

      this.isSpeaking.set(true);
      this.speakingMessageId.set(msgId);

      utterance.onend = () => {
        if (sessionId === this.currentSpeechSessionId) {
          this.isSpeaking.set(false);
          this.speakingMessageId.set(null);
        }
      };

      utterance.onerror = () => {
        if (sessionId === this.currentSpeechSessionId) {
          this.isSpeaking.set(false);
          this.speakingMessageId.set(null);
        }
      };

      window.speechSynthesis.speak(utterance);
    } catch {
      if (sessionId === this.currentSpeechSessionId) {
        this.isSpeaking.set(false);
        this.speakingMessageId.set(null);
      }
    }
  }

  stopSpeech(keepTypewriter: boolean = false): void {
    this.currentSpeechSessionId++;

    if (!keepTypewriter && this.typewriterInterval) {
      clearInterval(this.typewriterInterval);
      this.typewriterInterval = null;
    }

    if (this.currentTtsSub) {
      this.currentTtsSub.unsubscribe();
      this.currentTtsSub = null;
    }

    if (this.currentAudioPlayer) {
      try {
        this.currentAudioPlayer.pause();
        this.currentAudioPlayer.removeAttribute('src');
        this.currentAudioPlayer.load();
      } catch {}
      this.currentAudioPlayer = null;
    }

    if (this.currentAudioUrl) {
      try {
        URL.revokeObjectURL(this.currentAudioUrl);
      } catch {}
      this.currentAudioUrl = null;
    }

    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      try {
        window.speechSynthesis.cancel();
      } catch {}
    }

    this.isSpeaking.set(false);
    this.speakingMessageId.set(null);
  }

  ngOnDestroy(): void {
    this.stopSpeech();
    if (this.aiAdvisorSub) {
      this.aiAdvisorSub.unsubscribe();
      this.aiAdvisorSub = null;
    }
    if (this.recognition) {
      try {
        this.recognition.stop();
      } catch {}
    }
  }

  loadDetailedAnalytics(): void {
    if (!this.managerService?.getDetailedAnalytics) return;

    this.managerService.getDetailedAnalytics().subscribe({
      next: (data: any) => {
        // 1. Unidades / Yard Snapshots (Mapeamento resiliente de yardKey/yardLabel)
        if (data.yardSnapshots && data.yardSnapshots.length > 0) {
          const mappedYards: YardBiSnapshot[] = data.yardSnapshots.map((y: any) => {
            const key = y.key || y.yardKey || 'ALL';
            const rawLabel = y.label || y.yardLabel || '';
            const label = rawLabel.length > 0 ? rawLabel : (key === 'ALL' ? 'Todas as Unidades (SP)' : key);
            const total = y.totalInspections || 0;
            const deflection = y.deflectionRatePct !== undefined ? `${y.deflectionRatePct}%` : (y.aiPrecisionPct || '79.2%');
            const avgSla = y.averageResolutionTimeMinutes !== undefined ? `${y.averageResolutionTimeMinutes}m` : (y.avgSlaSeconds || '4,2s');
            const rev = y.auditedRevenueBrl || `R$ ${(total * 38).toLocaleString('pt-BR')},00`;
            return {
              key,
              label,
              totalInspections: total,
              aiPrecisionPct: deflection,
              avgSlaSeconds: avgSla,
              auditedRevenueBrl: rev,
              naturalWearCount: y.naturalWearCount ?? Math.round(total * 0.52),
              preExistingCount: y.preExistingCount ?? Math.round(total * 0.27),
              badUseCount: y.badUseCount ?? Math.round(total * 0.21),
            };
          });
          this.yardSnapshotsSignal.set(mappedYards);
        }

        // 2. Volume Diário (Mapeamento resiliente de dayLabel e proporções)
        if (data.dailyVolumeBars && data.dailyVolumeBars.length > 0) {
          const mappedBars: DailyInspectionBar[] = data.dailyVolumeBars.map((b: any) => {
            const day = b.day || b.dayLabel || '';
            const auto = b.autoCount || 0;
            const human = b.humanCount || 0;
            const total = b.totalCount || (auto + human) || 1;
            const direct = b.directPct !== undefined ? b.directPct : Math.round((auto * 100) / total);
            const workshop = b.workshopPct !== undefined ? b.workshopPct : Math.max(0, 100 - direct);
            return {
              day: day.length > 5 ? day.split(',')[0] : day,
              directPct: direct,
              workshopPct: workshop,
              totalCount: b.totalCount !== undefined ? b.totalCount : (auto + human),
            };
          });
          this.dailyVolumeBarsSignal.set(mappedBars);
        }

        // 3. Distribuição de Pareceres (Garante barClass e cores)
        if (data.verdictBreakdown && data.verdictBreakdown.length > 0) {
          const mappedVerdicts: VerdictBreakdownRow[] = data.verdictBreakdown.map((v: any) => {
            let barClass = 'bg-[#05944F]';
            let dotColor = '#05944F';
            let subtitle = 'Pastilhas, cabos e relação cobertos pelo plano';
            const labelLower = (v.label || v.verdict || '').toLowerCase();
            if (v.verdict === 'PRE_EXISTENTE' || labelLower.includes('pré-existente')) {
              barClass = 'bg-[#141414]';
              dotColor = '#141414';
              subtitle = 'Protegido pela janela de 48h ou 100 km';
            } else if (v.verdict === 'MAU_USO' || labelLower.includes('mau uso') || labelLower.includes('queda') || labelLower.includes('colis')) {
              barClass = 'bg-[#D97706]';
              dotColor = '#D97706';
              subtitle = 'Orçado pela tabela oficial da Loja Mottu';
            }
            return {
              label: v.label || v.verdict,
              subtitle: v.subtitle || subtitle,
              percentage: v.percentage || 0,
              count: v.count || 0,
              barClass,
              dotColor,
            };
          });
          this.verdictBreakdownSignal.set(mappedVerdicts);
        }

        // 4. Vistorias por Modelo da Frota (Garante sharePct para barra verde)
        if (data.fleetModelBreakdown && data.fleetModelBreakdown.length > 0) {
          const totalInsp = data.fleetModelBreakdown.reduce((acc: number, m: any) => acc + (m.inspectionsCount || 0), 0) || 1;
          const mappedModels: FleetModelAnalyticsRow[] = data.fleetModelBreakdown.map((m: any) => {
            const count = m.inspectionsCount || 0;
            const share = m.sharePct !== undefined ? m.sharePct : Math.round((count * 100) / totalInsp);
            return {
              modelName: m.modelName || m.modelKey,
              code: m.code || m.modelKey,
              inspectionsCount: count,
              sharePct: share,
              aiDeflectionPct: m.aiDeflectionPct ?? m.deflectionPct ?? 80,
              avgSlaSeconds: m.avgSlaSeconds || `${m.slaMinutes || 25}min`,
            };
          });
          this.fleetModelBreakdownSignal.set(mappedModels);
        }

        // 5. Peças Mais Recorrentes (Garante isNaturalWear e sharePct)
        if (data.topAuditedParts && data.topAuditedParts.length > 0) {
          const maxOcc = Math.max(...data.topAuditedParts.map((p: any) => p.occurrences || 0), 1);
          const mappedParts: AuditedPartAnalyticsRow[] = data.topAuditedParts.map((p: any) => {
            const isWear = p.isNaturalWear !== undefined ? p.isNaturalWear : (p.wearType === 'DESGASTE_NATURAL');
            const occ = p.occurrences || 0;
            const share = p.sharePct !== undefined ? p.sharePct : Math.min(100, Math.round((occ * 100) / maxOcc) + 20);
            const price = p.unitPriceBrl || 25;
            return {
              sku: p.sku,
              partName: p.partName,
              modelName: p.modelName,
              isNaturalWear: isWear,
              occurrences: occ,
              sharePct: share,
              totalAuditedBrl: p.totalAuditedBrl || `R$ ${(occ * price).toFixed(2).replace('.', ',')}`,
            };
          });
          this.topAuditedPartsSignal.set(mappedParts);
        }
      },
      error: () => {},
    });
  }

  loadAiAdvisor(): void {
    if (!this.managerService?.getAiAdvisorAdvice) return;
    this.aiAdvisorLoading.set(true);

    if (this.aiAdvisorSub) {
      this.aiAdvisorSub.unsubscribe();
      this.aiAdvisorSub = null;
    }

    this.aiAdvisorSub = this.managerService
      .getAiAdvisorAdvice(
        this.selectedYardFilter(),
        this.selectedHorizon(),
        this.selectedSystemCategory(),
        this.selectedScenario()
      )
      .subscribe({
        next: (res) => {
          if (res) this.aiAdvisor.set(res);
          this.aiAdvisorLoading.set(false);
          this.aiAdvisorSub = null;
        },
        error: () => {
          this.aiAdvisorLoading.set(false);
          this.aiAdvisorSub = null;
        },
      });
  }

  setHorizon(days: number): void {
    this.selectedHorizon.set(days);
    this.loadAiAdvisor();
  }

  setSystemCategory(category: string): void {
    this.selectedSystemCategory.set(category);
    this.loadAiAdvisor();
  }

  setScenario(scenario: string): void {
    this.selectedScenario.set(scenario);
    this.loadAiAdvisor();
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
