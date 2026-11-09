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
});
