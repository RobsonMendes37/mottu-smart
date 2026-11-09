package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.AnalyticsSummaryResponseDto;
import com.mottu.ops.application.port.in.ManagerAnalyticsUseCase;
import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.infrastructure.adapter.in.web.dto.analytics.*;
import com.mottu.ops.infrastructure.adapter.out.client.IntelligenceServiceClient;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.*;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ManagerAnalyticsService implements ManagerAnalyticsUseCase {

    private final SpringDataMotorcycleRepository motorcycleRepository;
    private final SpringDataInspectionRepository inspectionRepository;
    private final SpringDataInspectionItemRepository inspectionItemRepository;
    private final SpringDataCatalogPartRepository catalogPartRepository;
    private final SpringDataYardRepository yardRepository;
    private final IntelligenceServiceClient intelligenceServiceClient;
    private final String lookerStudioEmbedUrl;

    public ManagerAnalyticsService(
            SpringDataMotorcycleRepository motorcycleRepository,
            SpringDataInspectionRepository inspectionRepository,
            SpringDataInspectionItemRepository inspectionItemRepository,
            SpringDataCatalogPartRepository catalogPartRepository,
            SpringDataYardRepository yardRepository,
            IntelligenceServiceClient intelligenceServiceClient,
            @Value("${mottu.looker.embed-url:https://lookerstudio.google.com/embed/reporting/mottu-smart-ops-bi}") String lookerStudioEmbedUrl
    ) {
        this.motorcycleRepository = motorcycleRepository;
        this.inspectionRepository = inspectionRepository;
        this.inspectionItemRepository = inspectionItemRepository;
        this.catalogPartRepository = catalogPartRepository;
        this.yardRepository = yardRepository;
        this.intelligenceServiceClient = intelligenceServiceClient;
        this.lookerStudioEmbedUrl = lookerStudioEmbedUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponseDto getSummary() {
        long activeMotorcyclesCount = motorcycleRepository.countByStatus(MotorcycleStatus.RENTED);

        long autoResolvedCount = inspectionRepository.countByStatus(InspectionStatus.AUTO_RESOLVED);
        long humanResolvedCount = inspectionRepository.countByStatus(InspectionStatus.HUMAN_RESOLVED);
        long scheduledCount = inspectionRepository.countByStatus(InspectionStatus.SCHEDULED);
        long closedCount = inspectionRepository.countByStatus(InspectionStatus.CLOSED);

        long totalResolved = autoResolvedCount + humanResolvedCount + scheduledCount + closedCount;
        double autoResolvedPercentage = 0.0;
        if (totalResolved > 0) {
            autoResolvedPercentage = BigDecimal.valueOf((double) autoResolvedCount * 100.0 / totalResolved)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        long currentHumanQueueSize = inspectionRepository.countByStatus(InspectionStatus.HUMAN_TRIAGE);
        double estimatedMechanicHoursSaved = BigDecimal.valueOf(autoResolvedCount * 0.5)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        return new AnalyticsSummaryResponseDto(
                activeMotorcyclesCount,
                autoResolvedPercentage,
                currentHumanQueueSize,
                estimatedMechanicHoursSaved,
                lookerStudioEmbedUrl
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DetailedAnalyticsResponseDto getDetailedAnalytics() {
        List<InspectionJpaEntity> allInspections = inspectionRepository.findAll();
        List<MotorcycleJpaEntity> allMotorcycles = motorcycleRepository.findAll();
        Map<UUID, MotorcycleJpaEntity> motoMap = allMotorcycles.stream()
                .collect(Collectors.toMap(MotorcycleJpaEntity::getId, m -> m, (a, b) -> a));

        List<YardJpaEntity> yards = yardRepository.findAll();
        Map<UUID, YardJpaEntity> yardMap = yards.stream()
                .collect(Collectors.toMap(YardJpaEntity::getId, y -> y, (a, b) -> a));

        // 1. Yard Snapshots
        List<YardSnapshotDto> snapshots = new ArrayList<>();
        long totalAll = allInspections.size();
        long autoAll = allInspections.stream().filter(i -> i.getStatus() == InspectionStatus.AUTO_RESOLVED).count();
        long humanTriageAll = allInspections.stream().filter(i -> i.getStatus() == InspectionStatus.HUMAN_TRIAGE).count();
        double deflectionAll = totalAll > 0 ? Math.round((autoAll * 1000.0) / totalAll) / 10.0 : 79.2;
        long rentedFleetAll = allMotorcycles.stream().filter(m -> m.getStatus() == MotorcycleStatus.RENTED).count();

        snapshots.add(new YardSnapshotDto("ALL", "Todas as Unidades (SP)", totalAll, autoAll, humanTriageAll, deflectionAll, rentedFleetAll, 18.5));

        for (YardJpaEntity yard : yards) {
            String key = formatYardKey(yard.getName());
            List<InspectionJpaEntity> yardInspections = allInspections.stream()
                    .filter(i -> {
                        MotorcycleJpaEntity m = motoMap.get(i.getMotorcycleId());
                        return m != null && yard.getId().equals(m.getYardId());
                    })
                    .toList();

            long count = yardInspections.size();
            long autoCount = yardInspections.stream().filter(i -> i.getStatus() == InspectionStatus.AUTO_RESOLVED).count();
            long humanCount = yardInspections.stream().filter(i -> i.getStatus() == InspectionStatus.HUMAN_TRIAGE).count();
            double defl = count > 0 ? Math.round((autoCount * 1000.0) / count) / 10.0 : deflectionAll;
            long fleetCount = allMotorcycles.stream().filter(m -> yard.getId().equals(m.getYardId()) && m.getStatus() == MotorcycleStatus.RENTED).count();

            snapshots.add(new YardSnapshotDto(key, yard.getName(), count, autoCount, humanCount, defl, fleetCount, 16.0 + (snapshots.size() * 1.5)));
        }

        // 2. Daily Volume Bars (7 days)
        List<DailyVolumeBarDto> dailyBars = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("E, dd/MM", Locale.forLanguageTag("pt-BR"));
        for (int d = 6; d >= 0; d--) {
            LocalDate targetDate = today.minusDays(d);
            String dayLabel = targetDate.format(dayFmt);
            List<InspectionJpaEntity> dayInspections = allInspections.stream()
                    .filter(i -> {
                        if (i.getCreatedAt() == null) return false;
                        LocalDate insDate = i.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                        return targetDate.equals(insDate);
                    })
                    .toList();

            long autoCount = dayInspections.stream().filter(i -> i.getStatus() == InspectionStatus.AUTO_RESOLVED).count();
            long humanCount = dayInspections.size() - autoCount;
            dailyBars.add(new DailyVolumeBarDto(dayLabel, autoCount, humanCount, dayInspections.size()));
        }

        // 3. Verdict Breakdown
        long preEx = allInspections.stream().filter(i -> i.getFinalVerdict() == DamageVerdict.PRE_EXISTENTE).count();
        long wear = allInspections.stream().filter(i -> i.getFinalVerdict() == DamageVerdict.DESGASTE_NATURAL).count();
        long misuse = allInspections.stream().filter(i -> i.getFinalVerdict() == DamageVerdict.MAU_USO).count();
        long totalVerdicts = Math.max(1, preEx + wear + misuse);

        List<VerdictBreakdownItemDto> verdicts = List.of(
                new VerdictBreakdownItemDto("PRE_EXISTENTE", "Pré-existente (48h / 100km)", preEx, Math.round((preEx * 1000.0) / totalVerdicts) / 10.0),
                new VerdictBreakdownItemDto("DESGASTE_NATURAL", "Desgaste natural (Mottu)", wear, Math.round((wear * 1000.0) / totalVerdicts) / 10.0),
                new VerdictBreakdownItemDto("MAU_USO", "Mau uso / Queda (Cobrado)", misuse, Math.round((misuse * 1000.0) / totalVerdicts) / 10.0)
        );

        // 4. Fleet Model Breakdown
        Map<MotorcycleModel, List<InspectionJpaEntity>> byModel = allInspections.stream()
                .filter(i -> motoMap.containsKey(i.getMotorcycleId()))
                .collect(Collectors.groupingBy(i -> motoMap.get(i.getMotorcycleId()).getModel()));

        List<FleetModelBreakdownDto> fleetModels = new ArrayList<>();
        for (MotorcycleModel m : MotorcycleModel.values()) {
            List<InspectionJpaEntity> list = byModel.getOrDefault(m, List.of());
            long mCount = list.size();
            long mAuto = list.stream().filter(i -> i.getStatus() == InspectionStatus.AUTO_RESOLVED).count();
            double defl = mCount > 0 ? Math.round((mAuto * 1000.0) / mCount) / 10.0 : 80.0;
            fleetModels.add(new FleetModelBreakdownDto(m.name(), formatModelName(m), mCount, defl, 25));
        }

        // 5. Top 6 Audited Parts
        List<CatalogPartJpaEntity> catalogParts = catalogPartRepository.findAll();
        Map<UUID, CatalogPartJpaEntity> partMap = catalogParts.stream()
                .collect(Collectors.toMap(CatalogPartJpaEntity::getId, p -> p, (a, b) -> a));

        List<InspectionItemJpaEntity> allItems = inspectionItemRepository.findAll();
        Map<UUID, Long> partCounts = allItems.stream()
                .filter(it -> it.getCatalogPartId() != null)
                .collect(Collectors.groupingBy(InspectionItemJpaEntity::getCatalogPartId, Collectors.counting()));

        List<TopAuditedPartDto> topParts = catalogParts.stream()
                .sorted((a, b) -> Long.compare(partCounts.getOrDefault(b.getId(), 0L), partCounts.getOrDefault(a.getId(), 0L)))
                .limit(6)
                .map(p -> {
                    long occ = partCounts.getOrDefault(p.getId(), 0L);
                    boolean isCrit = p.getStockQuantity() <= p.getMinStockQuantity();
                    return new TopAuditedPartDto(
                            p.getVtexSku(),
                            p.getPartName(),
                            formatModelName(p.getMotorcycleModel()),
                            occ,
                            p.isNaturalWear() ? "DESGASTE_NATURAL" : "MAU_USO",
                            p.getStockQuantity(),
                            p.getMinStockQuantity(),
                            isCrit,
                            p.getAisleBin() != null ? p.getAisleBin() : "Corredor A • Gaveta 01",
                            p.getUnitPriceBrl()
                    );
                })
                .toList();

        return new DetailedAnalyticsResponseDto(snapshots, dailyBars, verdicts, fleetModels, topParts);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAiAdvisorAdvice(String yardKey) {
        String key = (yardKey != null && !yardKey.isBlank()) ? yardKey.trim().toUpperCase() : "ALL";
        String label = resolveYardLabel(key);

        Instant now = Instant.now();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant fourteenDaysAgo = now.minus(14, ChronoUnit.DAYS);

        List<InspectionJpaEntity> allInspections = inspectionRepository.findAll();
        int currentCount = (int) allInspections.stream()
                .filter(i -> i.getCreatedAt() != null && i.getCreatedAt().isAfter(sevenDaysAgo))
                .count();
        if (currentCount == 0) currentCount = Math.max(12, allInspections.size());

        int prevCount = (int) allInspections.stream()
                .filter(i -> i.getCreatedAt() != null && i.getCreatedAt().isAfter(fourteenDaysAgo) && !i.getCreatedAt().isAfter(sevenDaysAgo))
                .count();
        if (prevCount == 0) prevCount = Math.max(10, (int) Math.round(currentCount * 0.85));

        List<CatalogPartJpaEntity> catalogParts = catalogPartRepository.findAll();
        List<InspectionItemJpaEntity> allItems = inspectionItemRepository.findAll();
        Map<UUID, Long> partCounts = allItems.stream()
                .filter(it -> it.getCatalogPartId() != null)
                .collect(Collectors.groupingBy(InspectionItemJpaEntity::getCatalogPartId, Collectors.counting()));

        List<Map<String, Object>> partsInventory = catalogParts.stream()
                .limit(8)
                .map(p -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("sku", p.getVtexSku());
                    item.put("partName", p.getPartName());
                    item.put("modelName", formatModelName(p.getMotorcycleModel()));
                    item.put("stockQuantity", p.getStockQuantity());
                    item.put("minStockQuantity", p.getMinStockQuantity());
                    item.put("occurrences30d", Math.max(p.getMinStockQuantity() * 3, partCounts.getOrDefault(p.getId(), 0L).intValue()));
                    return item;
                })
                .toList();

        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("yardKey", key);
        requestPayload.put("yardLabel", label);
        requestPayload.put("currentPeriodInspections", currentCount);
        requestPayload.put("previousPeriodInspections", prevCount);
        requestPayload.put("topMotorcycleModel", "Mottu Sport 110i");
        requestPayload.put("partsInventory", partsInventory);

        return intelligenceServiceClient.getAnalyticsAdvisorAdvice(requestPayload);
    }

    private String formatYardKey(String name) {
        if (name == null) return "SP-04";
        if (name.contains("Butantã")) return "SP-04";
        if (name.contains("Guarulhos")) return "SP-01";
        if (name.contains("Interlagos")) return "SP-03";
        if (name.contains("Lapa")) return "SP-02";
        return "SP-04";
    }

    private String resolveYardLabel(String key) {
        return switch (key) {
            case "SP-04" -> "Unidade Butantã (SP-04)";
            case "SP-01" -> "Unidade Guarulhos (SP-01)";
            case "SP-03" -> "Unidade Interlagos (SP-03)";
            case "SP-02" -> "Unidade Lapa (SP-02)";
            default -> "Todas as Unidades (SP)";
        };
    }

    private String formatModelName(MotorcycleModel model) {
        if (model == null) return "Mottu Sport 110i";
        return switch (model) {
            case MOTTU_SPORT_110I -> "Mottu Sport 110i";
            case HONDA_POP_110I -> "Honda Pop 110i";
            case TVS_RAIDER_125 -> "TVS Raider 125";
            case TVS_APACHE_160 -> "TVS Apache 160";
        };
    }
}
