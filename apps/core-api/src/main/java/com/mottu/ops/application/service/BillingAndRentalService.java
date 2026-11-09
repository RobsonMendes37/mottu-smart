package com.mottu.ops.application.service;

import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.service.WeeklyBillingCalculator;
import com.mottu.ops.infrastructure.adapter.in.web.dto.billing.*;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.*;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BillingAndRentalService {

    private final SpringDataRentalContractRepository rentalContractRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataMotorcycleRepository motorcycleRepository;
    private final SpringDataInspectionRepository inspectionRepository;
    private final SpringDataInspectionItemRepository inspectionItemRepository;
    private final SpringDataRiderDailyDeliveryRepository riderDailyDeliveryRepository;
    private final WeeklyBillingCalculator weeklyBillingCalculator = new WeeklyBillingCalculator();

    public BillingAndRentalService(
            SpringDataRentalContractRepository rentalContractRepository,
            SpringDataUserRepository userRepository,
            SpringDataMotorcycleRepository motorcycleRepository,
            SpringDataInspectionRepository inspectionRepository,
            SpringDataInspectionItemRepository inspectionItemRepository,
            SpringDataRiderDailyDeliveryRepository riderDailyDeliveryRepository
    ) {
        this.rentalContractRepository = rentalContractRepository;
        this.userRepository = userRepository;
        this.motorcycleRepository = motorcycleRepository;
        this.inspectionRepository = inspectionRepository;
        this.inspectionItemRepository = inspectionItemRepository;
        this.riderDailyDeliveryRepository = riderDailyDeliveryRepository;
    }

    @Transactional(readOnly = true)
    public List<BillingContractSummaryDto> listBillingContracts() {
        List<RentalContractJpaEntity> contracts = rentalContractRepository.findAllByActiveTrue();
        List<BillingContractSummaryDto> list = new ArrayList<>();
        for (RentalContractJpaEntity contract : contracts) {
            list.add(toBillingSummary(contract));
        }
        return list;
    }

    @Transactional
    public BillingContractSummaryDto confirmPixPayment(String contractCode) {
        String cleanCode = contractCode.trim();
        RentalContractJpaEntity contract = rentalContractRepository.findByContractCode(cleanCode)
                .orElseGet(() -> rentalContractRepository.findAllByActiveTrue().stream()
                        .filter(c -> cleanCode.equalsIgnoreCase(c.getContractCode()) || cleanCode.equalsIgnoreCase(c.getId().toString()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado: " + cleanCode)));

        contract.setPaymentStatus("PAID");
        contract.setPaidAt(Instant.now());
        contract.setDueDateLabel("Pago via Pix • Confirmado");
        RentalContractJpaEntity saved = rentalContractRepository.save(contract);
        return toBillingSummary(saved);
    }

    @Transactional(readOnly = true)
    public RiderRentalSummaryResponseDto getRiderRentalSummary(UUID riderId) {
        RentalContractJpaEntity contract = rentalContractRepository.findByRiderIdAndActiveTrue(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum contrato ativo encontrado para o entregador: " + riderId));

        return buildRiderRentalSummary(contract);
    }

    @Transactional
    public RiderRentalSummaryResponseDto payRiderPix(UUID riderId) {
        RentalContractJpaEntity contract = rentalContractRepository.findByRiderIdAndActiveTrue(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum contrato ativo encontrado para o entregador: " + riderId));

        contract.setPaymentStatus("PAID");
        contract.setPaidAt(Instant.now());
        contract.setDueDateLabel("Pago via Pix • Confirmado");
        rentalContractRepository.save(contract);

        return buildRiderRentalSummary(contract);
    }

    @Transactional
    public RiderRentalSummaryResponseDto addDailyDeliveries(UUID riderId, AddRiderDeliveriesRequestDto request) {
        RentalContractJpaEntity contract = rentalContractRepository.findByRiderIdAndActiveTrue(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum contrato ativo encontrado para o entregador: " + riderId));

        LocalDate today = LocalDate.now();
        Optional<RiderDailyDeliveryJpaEntity> existingOpt = riderDailyDeliveryRepository.findByContractIdAndDeliveryDate(contract.getId(), today);

        if (existingOpt.isPresent()) {
            RiderDailyDeliveryJpaEntity existing = existingOpt.get();
            existing.setCompletedCount(existing.getCompletedCount() + request.completedCount());
            existing.setEarningsBrl(existing.getEarningsBrl().add(request.earningsBrl()));
            riderDailyDeliveryRepository.save(existing);
        } else {
            RiderDailyDeliveryJpaEntity newDelivery = new RiderDailyDeliveryJpaEntity(
                    UUID.randomUUID(),
                    riderId,
                    contract.getId(),
                    "Hoje (" + today.format(DateTimeFormatter.ofPattern("dd/MM")) + ")",
                    request.completedCount(),
                    request.earningsBrl(),
                    "98%",
                    today,
                    Instant.now()
            );
            riderDailyDeliveryRepository.save(newDelivery);
        }

        return buildRiderRentalSummary(contract);
    }

    private BillingContractSummaryDto toBillingSummary(RentalContractJpaEntity contract) {
        String riderName = userRepository.findById(contract.getRiderId())
                .map(UserJpaEntity::getFullName)
                .orElse("Entregador Parceiro");

        String plate = "MOT-OPS";
        String model = "Mottu Sport 110i";
        MotorcycleModel motoModel = MotorcycleModel.MOTTU_SPORT_110I;

        Optional<MotorcycleJpaEntity> motoOpt = motorcycleRepository.findById(contract.getMotorcycleId());
        if (motoOpt.isPresent()) {
            MotorcycleJpaEntity moto = motoOpt.get();
            plate = moto.getPlate();
            motoModel = moto.getModel();
            model = formatModelName(motoModel);
        }

        BigDecimal basePlan = contract.getWeeklyPlanBrl() != null
                ? contract.getWeeklyPlanBrl()
                : weeklyBillingCalculator.resolveBaseWeeklyPlanBrl(motoModel);

        BigDecimal discount = contract.getDiscountBrl() != null
                ? contract.getDiscountBrl()
                : new BigDecimal("15.00");

        List<InspectionJpaEntity> inspections = inspectionRepository.findByContractId(contract.getId());
        BigDecimal charges = inspections.stream()
                .filter(i -> i.getFinalVerdict() == DamageVerdict.MAU_USO)
                .map(i -> i.getTotalEstimatedBrl() != null ? i.getTotalEstimatedBrl() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RiderDailyDeliveryJpaEntity> deliveries = riderDailyDeliveryRepository.findByContractIdOrderByDeliveryDateAsc(contract.getId());
        int weeklyDeliveriesCount = deliveries.stream().mapToInt(RiderDailyDeliveryJpaEntity::getCompletedCount).sum();

        BigDecimal bonus = weeklyBillingCalculator.calculateDeliveryGoalBonusBrl(weeklyDeliveriesCount);
        BigDecimal netTotal = weeklyBillingCalculator.calculateNetWeeklyTotal(basePlan, charges, discount, weeklyDeliveriesCount);

        String code = contract.getContractCode() != null
                ? contract.getContractCode()
                : "CTR-2026-" + contract.getId().toString().replace("-", "").substring(0, 4);

        return new BillingContractSummaryDto(
                contract.getId(),
                code,
                riderName,
                plate,
                model,
                basePlan,
                charges,
                discount,
                bonus,
                netTotal,
                contract.getPaymentStatus() != null ? contract.getPaymentStatus() : "PENDING_PIX",
                contract.getDueDateLabel() != null ? contract.getDueDateLabel() : "Vence segunda-feira • 18:00"
        );
    }

    private RiderRentalSummaryResponseDto buildRiderRentalSummary(RentalContractJpaEntity contract) {
        String plate = "MOT1A23";
        String model = "Mottu Sport 110i";
        MotorcycleModel motoModel = MotorcycleModel.MOTTU_SPORT_110I;

        Optional<MotorcycleJpaEntity> motoOpt = motorcycleRepository.findById(contract.getMotorcycleId());
        if (motoOpt.isPresent()) {
            MotorcycleJpaEntity moto = motoOpt.get();
            plate = moto.getPlate();
            motoModel = moto.getModel();
            model = formatModelName(motoModel);
        }

        BigDecimal basePlan = contract.getWeeklyPlanBrl() != null
                ? contract.getWeeklyPlanBrl()
                : weeklyBillingCalculator.resolveBaseWeeklyPlanBrl(motoModel);

        BigDecimal discount = contract.getDiscountBrl() != null
                ? contract.getDiscountBrl()
                : new BigDecimal("15.00");

        List<InspectionJpaEntity> inspections = inspectionRepository.findByContractId(contract.getId());
        List<RiderChargedInspectionDto> chargedInspections = new ArrayList<>();
        BigDecimal charges = BigDecimal.ZERO;

        for (InspectionJpaEntity insp : inspections) {
            if (insp.getFinalVerdict() == DamageVerdict.MAU_USO) {
                BigDecimal amount = insp.getTotalEstimatedBrl() != null ? insp.getTotalEstimatedBrl() : BigDecimal.ZERO;
                charges = charges.add(amount);

                List<InspectionItemJpaEntity> items = inspectionItemRepository.findByInspectionId(insp.getId());
                String partName = items.isEmpty() ? "Peça avariada" : items.getFirst().getPartNameSnapshot();
                String dateStr = insp.getCreatedAt() != null
                        ? DateTimeFormatter.ofPattern("dd/MM/yyyy").format(insp.getCreatedAt().atZone(ZoneId.systemDefault()))
                        : "Recente";

                chargedInspections.add(new RiderChargedInspectionDto(
                        insp.getId(),
                        dateStr,
                        partName,
                        "MAU_USO",
                        amount
                ));
            }
        }

        List<RiderDailyDeliveryJpaEntity> deliveryEntities = riderDailyDeliveryRepository.findByContractIdOrderByDeliveryDateAsc(contract.getId());
        int weeklyDeliveriesTotal = deliveryEntities.stream().mapToInt(RiderDailyDeliveryJpaEntity::getCompletedCount).sum();
        BigDecimal weeklyEarnings = deliveryEntities.stream()
                .map(d -> d.getEarningsBrl() != null ? d.getEarningsBrl() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RiderDailyDeliveryItemDto> deliveryDays = deliveryEntities.stream()
                .map(d -> new RiderDailyDeliveryItemDto(d.getDayLabel(), d.getCompletedCount(), d.getEarningsBrl(), d.getOnTimeRate()))
                .toList();

        BigDecimal bonus = weeklyBillingCalculator.calculateDeliveryGoalBonusBrl(weeklyDeliveriesTotal);
        BigDecimal netTotal = weeklyBillingCalculator.calculateNetWeeklyTotal(basePlan, charges, discount, weeklyDeliveriesTotal);

        String code = contract.getContractCode() != null
                ? contract.getContractCode()
                : "CTR-2026-" + contract.getId().toString().replace("-", "").substring(0, 4);

        return new RiderRentalSummaryResponseDto(
                code,
                plate,
                model,
                basePlan,
                discount,
                bonus,
                charges,
                netTotal,
                contract.getPaymentStatus() != null ? contract.getPaymentStatus() : "PENDING_PIX",
                contract.getDueDateLabel() != null ? contract.getDueDateLabel() : "Vence segunda-feira • 18:00",
                chargedInspections,
                weeklyDeliveriesTotal,
                weeklyEarnings,
                120,
                deliveryDays
        );
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

