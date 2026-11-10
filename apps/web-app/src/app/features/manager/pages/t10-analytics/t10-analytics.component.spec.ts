import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { T10AnalyticsComponent } from './t10-analytics.component';
import { AnalyticsSummaryResponseDto } from '../../models/manager.models';

describe('T10AnalyticsComponent (Screen T10 - Looker Studio Executive Analytics)', () => {
  let comp: T10AnalyticsComponent;
  let mockManagerService: any;
  let mockSanitizer: any;

  const mockSummaryWithEmbed: AnalyticsSummaryResponseDto = {
    activeMotorcyclesCount: 124,
    autoResolvedPercentage: 78.5,
    currentHumanQueueSize: 4,
    estimatedMechanicHoursSaved: 36.2,
    lookerStudioEmbedUrl: 'https://lookerstudio.google.com/embed/reporting/mottu-smart-ops-bi',
  };

  const mockSummaryEmptyEmbed: AnalyticsSummaryResponseDto = {
    activeMotorcyclesCount: 88,
    autoResolvedPercentage: 81.2,
    currentHumanQueueSize: 2,
    estimatedMechanicHoursSaved: 24.5,
    lookerStudioEmbedUrl: null,
  };

  beforeEach(() => {
    mockManagerService = {
      getAnalyticsSummary: vi.fn().mockReturnValue(of(mockSummaryWithEmbed)),
    };
    mockSanitizer = {
      bypassSecurityTrustResourceUrl: vi.fn().mockImplementation((url: string) => `safe:${url}`),
    };

    comp = new T10AnalyticsComponent(mockManagerService, mockSanitizer);
  });

  it('TC-T10-1: ngOnInit loads analytics summary and populates 4 KPI cards', () => {
    comp.ngOnInit();
    expect(mockManagerService.getAnalyticsSummary).toHaveBeenCalled();
    expect(comp.summary()?.activeMotorcyclesCount).toBe(124);
    expect(comp.summary()?.autoResolvedPercentage).toBe(78.5);
    expect(comp.summary()?.currentHumanQueueSize).toBe(4);
    expect(comp.summary()?.estimatedMechanicHoursSaved).toBe(36.2);
    expect(comp.isLoading()).toBe(false);
  });

  it('TC-T10-2: Sanitizes lookerStudioEmbedUrl when present', () => {
    comp.ngOnInit();
    expect(mockSanitizer.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(
      'https://lookerstudio.google.com/embed/reporting/mottu-smart-ops-bi'
    );
    expect(comp.safeEmbedUrl()).toBe(
      'safe:https://lookerstudio.google.com/embed/reporting/mottu-smart-ops-bi'
    );
    expect(comp.isOfflineFallback()).toBe(false);
  });

  it('TC-T10-3: Activates offline fallback mode when lookerStudioEmbedUrl is null or empty', () => {
    mockManagerService.getAnalyticsSummary.mockReturnValue(of(mockSummaryEmptyEmbed));
    comp.loadSummary();

    expect(comp.summary()?.activeMotorcyclesCount).toBe(88);
    expect(comp.isOfflineFallback()).toBe(true);
    expect(comp.safeEmbedUrl()).toBeNull();
  });

  it('TC-T10-4: Refresh button reloads summary data', () => {
    comp.refresh();
    expect(mockManagerService.getAnalyticsSummary).toHaveBeenCalled();
  });

  it('TC-T10-5: Handles error gracefully by setting errorMessage', () => {
    mockManagerService.getAnalyticsSummary.mockReturnValue(throwError(() => new Error('Network error')));
    comp.loadSummary();

    expect(comp.errorMessage()).toContain('Erro ao carregar indicadores executivos');
    expect(comp.isLoading()).toBe(false);
  });

  it('TC-T10-6: Exposes daily volume bars and verdict breakdown adhering to Purple Ban', () => {
    expect(comp.dailyVolumeBars.length).toBe(7);
    expect(comp.verdictBreakdown.length).toBe(3);
    for (const item of comp.verdictBreakdown) {
      expect(item.barClass).not.toMatch(/purple|violet|indigo/);
    }
  });

  it('TC-T10-7: Switches between native Mottu BI Studio and Looker Studio iframe mode and filters by yard', () => {
    expect(comp.biViewMode()).toBe('native');
    expect(comp.selectedYardFilter()).toBe('ALL');
    expect(comp.activeBiSnapshot().totalInspections).toBe(480);

    comp.setYardFilter('SP-04');
    expect(comp.selectedYardFilter()).toBe('SP-04');
    expect(comp.activeBiSnapshot().totalInspections).toBe(168);

    comp.setBiViewMode('looker');
    expect(comp.biViewMode()).toBe('looker');
    expect(comp.fleetModelBreakdown.length).toBe(4);
    expect(comp.topAuditedParts.length).toBe(6);
  });

  it('TC-T10-8: Exposes AI Ops Advisor insights with BigQuery forecasting', () => {
    expect(comp.aiAdvisor()).toBeDefined();
    expect(comp.aiAdvisor().modelUsed).toContain('gemini');
    expect(comp.aiAdvisor().demandGrowthPct).toBeGreaterThan(0);
    expect(comp.aiAdvisor().stockoutAlerts.length).toBeGreaterThan(0);
    expect(comp.aiAdvisor().recommendedActions.length).toBeGreaterThan(0);
  });

  it('TC-T10-9: restockPart performs 1-click batch replenishment and updates risk level to HEALTHY', () => {
    const critical = comp.aiAdvisor().stockoutAlerts.find((a) => a.riskLevel === 'CRITICAL')!;
    expect(critical).toBeDefined();
    const initialQty = critical.currentStock;

    comp.restockPart(critical.sku, critical.suggestedRestockQty);

    const updated = comp.aiAdvisor().stockoutAlerts.find((a) => a.sku === critical.sku)!;
    expect(updated.currentStock).toBe(initialQty + critical.suggestedRestockQty);
    expect(updated.riskLevel).toBe('HEALTHY');
    expect(comp.aiAdvisorSuccessBanner()).toContain('reposto com sucesso');
  });

  it('TC-T10-10: Supports scrollToSection safely when element is present or absent', () => {
    expect(() => comp.scrollToSection('verdict-distribution-section')).not.toThrow();
  });

  it('TC-T10-11: executeDataQuery fetches structured analytics from managerService', () => {
    mockManagerService.askDataExplorer = vi.fn().mockReturnValue(
      of({
        query: 'estoque crítico',
        yardKey: 'ALL',
        yardLabel: 'Todas as Unidades (SP)',
        directAnswer: 'Foram encontrados 5 SKUs críticos',
        keyMetricLabel: 'SKUs em Ruptura',
        keyMetricValue: '5 Críticos',
        keyMetricTrend: 'Reposição necessária',
        columns: ['SKU', 'Peça'],
        rows: [['SKU-01', 'Manete']],
        suggestedActionLabel: 'Repor no Almoxarifado',
        suggestedActionRoute: '/mechanic/operations',
      })
    );

    comp.executeDataQuery('estoque crítico');

    expect(mockManagerService.askDataExplorer).toHaveBeenCalledWith('estoque crítico', 'ALL');
    expect(comp.dataExplorerResult()?.directAnswer).toBe('Foram encontrados 5 SKUs críticos');
    expect(comp.dataExplorerLoading()).toBe(false);
  });

  it('TC-T10-12: copyExecutiveSummary and exportCsv run safely without error', () => {
    expect(() => comp.copyExecutiveSummary()).not.toThrow();
    expect(() => comp.exportCsv()).not.toThrow();
  });

  it('TC-T10-13: Supports interactive predictive parameters: setHorizon, setSystemCategory, setScenario', () => {
    mockManagerService.getAiAdvisorAdvice = vi.fn().mockReturnValue(of({
      ...comp.aiAdvisor(),
      horizonDays: 15,
      scenario: 'RAIN_PEAK',
      systemCategory: 'BRAKE_HANDLEBAR',
      totalRestockBudgetBrl: 1500.0,
    }));

    comp.setHorizon(15);
    expect(comp.selectedHorizon()).toBe(15);
    expect(mockManagerService.getAiAdvisorAdvice).toHaveBeenCalledWith('ALL', 15, 'ALL', 'NORMAL');

    comp.setSystemCategory('BRAKE_HANDLEBAR');
    expect(comp.selectedSystemCategory()).toBe('BRAKE_HANDLEBAR');

    comp.setScenario('RAIN_PEAK');
    expect(comp.selectedScenario()).toBe('RAIN_PEAK');
    expect(mockManagerService.getAiAdvisorAdvice).toHaveBeenCalledWith('ALL', 15, 'BRAKE_HANDLEBAR', 'RAIN_PEAK');
  });

  it('TC-T10-14: Computes totalStockoutBudget and criticalAlertsCount dynamically', () => {
    expect(comp.totalStockoutBudget()).toBeGreaterThan(0);
    expect(comp.criticalAlertsCount()).toBeGreaterThanOrEqual(1);
  });

  it('TC-T10-15: Floating Voice Copilot Monalisa toggles, executes skills, sends messages, and manages audio', () => {
    expect(comp.isCopilotOpen()).toBe(false);
    expect(comp.autoVoiceEnabled()).toBe(true);
    expect(comp.operationalSkills.length).toBe(7);
    expect(comp.copilotMessages()[0].text).toContain('Monalisa');

    comp.toggleCopilot();
    expect(comp.isCopilotOpen()).toBe(true);

    // Toggle voice switch
    comp.toggleAutoVoice();
    expect(comp.autoVoiceEnabled()).toBe(false);
    comp.toggleAutoVoice();
    expect(comp.autoVoiceEnabled()).toBe(true);

    // Text speech sanitizer
    const sanitized = comp.cleanTextForSpeech('Troca de manete R$ 35,00 com 78,5% de deflexão em SP-04.');
    expect(sanitized).toContain('35 reais');
    expect(sanitized).toContain('78 vírgula 5 por cento');
    expect(sanitized).toContain('unidade Butantã');

    mockManagerService.askDataExplorer = vi.fn().mockReturnValue(of({
      query: 'Estoque Crítico',
      directAnswer: 'Demanda de reposição calculada com sucesso.',
      spokenAnswer: 'Três itens com estoque crítico no galpão.',
      shouldShowTable: true,
      keyMetricLabel: 'Peças em Falta',
      keyMetricValue: '3 SKUs Críticos',
      keyMetricTrend: 'Alta',
      yardLabel: 'Todas as Unidades (SP)',
      suggestedActionLabel: 'Ver peças',
      suggestedActionRoute: '/mechanic/operations',
      columns: ['SKU', 'Saldo'],
      rows: [['SKU-01', '4 un.']],
      activeSkill: 'SKILL_ESTOQUE',
      isAiOffline: false,
    }));

    // Execute skill
    comp.executeSkill('SKILL_ESTOQUE', 'Estoque Crítico');
    expect(mockManagerService.askDataExplorer).toHaveBeenCalledWith(
      'Estoque Crítico',
      'ALL',
      'SKILL_ESTOQUE',
      expect.any(Array)
    );
    expect(comp.copilotMessages().length).toBeGreaterThanOrEqual(2);
    const lastMsg = comp.copilotMessages()[comp.copilotMessages().length - 1];
    expect(lastMsg.text).toContain('Demanda de reposição');
    expect(lastMsg.spokenText).toBe('Três itens com estoque crítico no galpão.');
    expect(lastMsg.shouldShowTable).toBe(true);
    expect(lastMsg.rows?.length).toBe(1);

    // Audio methods run safely
    expect(() => comp.speakText('Teste de voz', 999)).not.toThrow();
    expect(() => comp.stopSpeech()).not.toThrow();
    expect(() => comp.toggleSpeechRecognition()).not.toThrow();
    expect(comp.isSpeaking()).toBe(false);

    comp.toggleCopilot();
    expect(comp.isCopilotOpen()).toBe(false);
  });

  it('TC-T10-16: loadDetailedAnalytics maps raw backend DTOs to UI models preserving bar colors and yard labels', () => {
    mockManagerService.getDetailedAnalytics = vi.fn().mockReturnValue(of({
      yardSnapshots: [
        { yardKey: 'SP-04', yardLabel: 'Galpão Mottu Butantã', totalInspections: 45, deflectionRatePct: 82.5 }
      ],
      dailyVolumeBars: [
        { dayLabel: 'seg., 09/11', autoCount: 10, humanCount: 2, totalCount: 12 }
      ],
      verdictBreakdown: [
        { verdict: 'DESGASTE_NATURAL', label: 'Desgaste natural (Mottu)', count: 25, percentage: 55.5 },
        { verdict: 'MAU_USO', label: 'Mau uso', count: 10, percentage: 22.2 }
      ],
      fleetModelBreakdown: [
        { modelKey: 'MOTTU_SPORT_110I', modelName: 'Mottu Sport 110i', inspectionsCount: 30, deflectionPct: 85.0 }
      ],
      topAuditedParts: [
        { sku: 'SKU-001', partName: 'Manete', modelName: 'Sport', occurrences: 5, wearType: 'DESGASTE_NATURAL', unitPriceBrl: 25 }
      ]
    }));

    comp.loadDetailedAnalytics();

    // Verify yard label is not blank ("bolinhas" bug eliminated)
    expect(comp.yardSnapshots[0].label).toBe('Galpão Mottu Butantã');
    expect(comp.yardSnapshots[0].key).toBe('SP-04');

    // Verify volume bar colors & proportions
    expect(comp.dailyVolumeBars[0].directPct).toBe(83);
    expect(comp.dailyVolumeBars[0].workshopPct).toBe(17);

    // Verify verdict bar classes for colors
    expect(comp.verdictBreakdown[0].barClass).toBe('bg-[#05944F]');
    expect(comp.verdictBreakdown[1].barClass).toBe('bg-[#D97706]');
  });
});
