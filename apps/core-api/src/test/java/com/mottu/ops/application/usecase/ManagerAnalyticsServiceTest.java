package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.AnalyticsSummaryResponseDto;
import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.infrastructure.adapter.in.web.dto.analytics.DetailedAnalyticsResponseDto;
import com.mottu.ops.infrastructure.adapter.out.client.IntelligenceServiceClient;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.*;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerAnalyticsServiceTest {

    @Mock
    private SpringDataMotorcycleRepository motorcycleRepository;

    @Mock
    private SpringDataInspectionRepository inspectionRepository;

    @Mock
    private SpringDataInspectionItemRepository inspectionItemRepository;

    @Mock
    private SpringDataCatalogPartRepository catalogPartRepository;

    @Mock
    private SpringDataYardRepository yardRepository;

    @Mock
    private IntelligenceServiceClient intelligenceServiceClient;

    private ManagerAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new ManagerAnalyticsService(
                motorcycleRepository,
                inspectionRepository,
                inspectionItemRepository,
                catalogPartRepository,
                yardRepository,
                intelligenceServiceClient,
                "https://lookerstudio.google.com/embed/test"
        );
    }

    @Test
    @DisplayName("getSummary() calculates active fleet, auto resolved percentage, and hours saved")
    void testGetSummary() {
        when(motorcycleRepository.countByStatus(MotorcycleStatus.RENTED)).thenReturn(40L);
        when(inspectionRepository.countByStatus(InspectionStatus.AUTO_RESOLVED)).thenReturn(80L);
        when(inspectionRepository.countByStatus(InspectionStatus.HUMAN_RESOLVED)).thenReturn(20L);
        when(inspectionRepository.countByStatus(InspectionStatus.SCHEDULED)).thenReturn(0L);
        when(inspectionRepository.countByStatus(InspectionStatus.CLOSED)).thenReturn(0L);
        when(inspectionRepository.countByStatus(InspectionStatus.HUMAN_TRIAGE)).thenReturn(5L);

        AnalyticsSummaryResponseDto summary = service.getSummary();

        assertNotNull(summary);
        assertEquals(40L, summary.activeMotorcyclesCount());
        assertEquals(80.0, summary.autoResolvedPercentage());
        assertEquals(5L, summary.currentHumanQueueSize());
        assertEquals(40.0, summary.estimatedMechanicHoursSaved());
        assertEquals("https://lookerstudio.google.com/embed/test", summary.lookerStudioEmbedUrl());
    }

    @Test
    @DisplayName("getDetailedAnalytics() succeeds with full data set")
    void testGetDetailedAnalyticsWithData() {
        UUID yardId = UUID.randomUUID();
        YardJpaEntity yard = new YardJpaEntity(yardId, "Galpão Butantã", "São Paulo", "Av Corifeu, 1000", true);
        when(yardRepository.findAll()).thenReturn(List.of(yard));

        UUID motoId = UUID.randomUUID();
        MotorcycleJpaEntity moto = new MotorcycleJpaEntity(motoId, "MOT1A23", MotorcycleModel.MOTTU_SPORT_110I, yardId, MotorcycleStatus.RENTED, 5000, Instant.now());
        when(motorcycleRepository.findAll()).thenReturn(List.of(moto));

        InspectionJpaEntity insp = new InspectionJpaEntity();
        insp.setId(UUID.randomUUID());
        insp.setMotorcycleId(motoId);
        insp.setStatus(InspectionStatus.AUTO_RESOLVED);
        insp.setFinalVerdict(DamageVerdict.DESGASTE_NATURAL);
        insp.setCreatedAt(Instant.now());
        when(inspectionRepository.findAll()).thenReturn(List.of(insp));

        CatalogPartJpaEntity part = new CatalogPartJpaEntity();
        part.setId(UUID.randomUUID());
        part.setVtexSku("SKU-TEST");
        part.setPartName("Lona de Freio");
        part.setMotorcycleModel(MotorcycleModel.MOTTU_SPORT_110I);
        part.setStockQuantity(5);
        part.setMinStockQuantity(8);
        part.setNaturalWear(true);
        part.setUnitPriceBrl(new BigDecimal("35.00"));
        when(catalogPartRepository.findAll()).thenReturn(List.of(part));
        when(inspectionItemRepository.findAll()).thenReturn(List.of());

        DetailedAnalyticsResponseDto res = service.getDetailedAnalytics();

        assertNotNull(res);
        assertFalse(res.yardSnapshots().isEmpty());
        assertEquals(7, res.dailyVolumeBars().size());
        assertEquals(3, res.verdictBreakdown().size());
        assertEquals(4, res.fleetModelBreakdown().size());
        assertEquals(1, res.topAuditedParts().size());
        assertTrue(res.topAuditedParts().getFirst().isCritical());
    }

    @Test
    @DisplayName("getDetailedAnalytics() never crashes and returns robust fallback on null or exception")
    void testGetDetailedAnalyticsDefensiveOnException() {
        when(inspectionRepository.findAll()).thenThrow(new RuntimeException("DB Connection Timeout"));

        DetailedAnalyticsResponseDto res = service.getDetailedAnalytics();

        assertNotNull(res);
        assertFalse(res.yardSnapshots().isEmpty());
        assertEquals(7, res.dailyVolumeBars().size());
        assertEquals(3, res.verdictBreakdown().size());
        assertEquals(4, res.fleetModelBreakdown().size());
        assertFalse(res.topAuditedParts().isEmpty());
    }

    @Test
    @DisplayName("getAiAdvisorAdvice() returns advice from intelligenceServiceClient")
    void testGetAiAdvisorAdviceSuccess() {
        when(inspectionRepository.findAll()).thenReturn(List.of());
        when(catalogPartRepository.findAll()).thenReturn(List.of());
        when(inspectionItemRepository.findAll()).thenReturn(List.of());

        Map<String, Object> advisorResponse = Map.of(
                "yardKey", "SP-04",
                "executiveHeadline", "Crescimento sustentável na Unidade Butantã",
                "recommendedActions", List.of("Ação 1", "Ação 2")
        );
        when(intelligenceServiceClient.getAnalyticsAdvisorAdvice(anyMap())).thenReturn(advisorResponse);

        Map<String, Object> advice = service.getAiAdvisorAdvice("SP-04");

        assertNotNull(advice);
        assertEquals("Crescimento sustentável na Unidade Butantã", advice.get("executiveHeadline"));
    }

    @Test
    @DisplayName("getAiAdvisorAdvice() never crashes on unexpected null or exception")
    void testGetAiAdvisorAdviceDefensiveFallback() {
        when(inspectionRepository.findAll()).thenThrow(new RuntimeException("Simulated Failure"));
        when(intelligenceServiceClient.getAnalyticsAdvisorAdvice(anyMap())).thenReturn(Map.of(
                "executiveHeadline", "Fallback seguro executivo ativado",
                "recommendedActions", List.of("Monitorar demanda")
        ));

        Map<String, Object> advice = service.getAiAdvisorAdvice(null);

        assertNotNull(advice);
        assertEquals("Fallback seguro executivo ativado", advice.get("executiveHeadline"));
    }
}

