package com.mottu.ops.application.service;

import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.domain.enums.ServiceOrderStatus;
import com.mottu.ops.domain.service.ServiceOrderPolicy;
import com.mottu.ops.infrastructure.adapter.in.web.dto.mechanic.*;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.CatalogPartJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionItemJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.MotorcycleJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.ServiceOrderJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MechanicOperationsService {

    private final SpringDataServiceOrderRepository serviceOrderRepository;
    private final SpringDataCatalogPartRepository catalogPartRepository;
    private final SpringDataMotorcycleRepository motorcycleRepository;
    private final SpringDataRentalContractRepository rentalContractRepository;
    private final SpringDataInspectionItemRepository inspectionItemRepository;
    private final ServiceOrderPolicy serviceOrderPolicy = new ServiceOrderPolicy();

    public MechanicOperationsService(
            SpringDataServiceOrderRepository serviceOrderRepository,
            SpringDataCatalogPartRepository catalogPartRepository,
            SpringDataMotorcycleRepository motorcycleRepository,
            SpringDataRentalContractRepository rentalContractRepository,
            SpringDataInspectionItemRepository inspectionItemRepository
    ) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.catalogPartRepository = catalogPartRepository;
        this.motorcycleRepository = motorcycleRepository;
        this.rentalContractRepository = rentalContractRepository;
        this.inspectionItemRepository = inspectionItemRepository;
    }

    @Transactional(readOnly = true)
    public MechanicOperationsResponseDto getOperationsSummary() {
        List<ServiceOrderJpaEntity> orders = serviceOrderRepository.findAllByOrderByCreatedAtDesc();
        List<ServiceOrderResponseDto> orderDtos = orders.stream()
                .map(this::toOrderDto)
                .toList();

        List<CatalogPartJpaEntity> parts = catalogPartRepository.findAll();
        List<StockItemResponseDto> stockDtos = parts.stream()
                .sorted(Comparator.comparingInt(CatalogPartJpaEntity::getStockQuantity))
                .map(this::toStockDto)
                .toList();

        return new MechanicOperationsResponseDto(orderDtos, stockDtos);
    }

    @Transactional
    public ServiceOrderResponseDto createServiceOrder(CreateServiceOrderRequestDto request) {
        String cleanPlate = request.plate().trim().toUpperCase();
        MotorcycleJpaEntity moto = motorcycleRepository.findByPlate(cleanPlate)
                .orElseThrow(() -> new ResourceNotFoundException("Motocicleta não encontrada com a placa: " + cleanPlate));

        String code = String.format("OS-2026-%04d", Math.abs(UUID.randomUUID().hashCode() % 9000) + 1000);
        while (serviceOrderRepository.findByCode(code).isPresent()) {
            code = String.format("OS-2026-%04d", Math.abs(UUID.randomUUID().hashCode() % 9000) + 1000);
        }

        ServiceOrderJpaEntity order = new ServiceOrderJpaEntity(
                UUID.randomUUID(),
                code,
                moto.getId(),
                null,
                moto.getYardId(),
                request.bay().trim(),
                request.serviceTitle().trim(),
                "Abertura manual de oficina",
                30,
                ServiceOrderStatus.WAITING_BAY,
                Instant.now(),
                Instant.now()
        );

        ServiceOrderJpaEntity saved = serviceOrderRepository.save(order);
        return toOrderDto(saved);
    }

    @Transactional
    public ServiceOrderResponseDto advanceOrderStatus(UUID orderId) {
        ServiceOrderJpaEntity order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada: " + orderId));

        ServiceOrderStatus nextStatus = serviceOrderPolicy.nextStatus(order.getStatus());
        order.setStatus(nextStatus);
        order.setUpdatedAt(Instant.now());

        boolean hasContract = rentalContractRepository.findByMotorcycleIdAndActiveTrue(order.getMotorcycleId()).isPresent();
        MotorcycleStatus newMotoStatus = serviceOrderPolicy.resolveMotorcycleStatusOnOrderTransition(nextStatus, hasContract);

        motorcycleRepository.findById(order.getMotorcycleId()).ifPresent(moto -> {
            moto.setStatus(newMotoStatus);
            motorcycleRepository.save(moto);
        });

        if (nextStatus == ServiceOrderStatus.READY_PICKUP && order.getInspectionId() != null) {
            List<InspectionItemJpaEntity> items = inspectionItemRepository.findByInspectionId(order.getInspectionId());
            for (InspectionItemJpaEntity item : items) {
                if (item.getCatalogPartId() != null) {
                    catalogPartRepository.findById(item.getCatalogPartId()).ifPresent(part -> {
                        int updated = serviceOrderPolicy.calculateUpdatedStock(part.getStockQuantity(), -1);
                        part.setStockQuantity(updated);
                        part.setUpdatedAt(Instant.now());
                        catalogPartRepository.save(part);
                    });
                }
            }
        }

        ServiceOrderJpaEntity saved = serviceOrderRepository.save(order);
        return toOrderDto(saved);
    }

    @Transactional
    public StockItemResponseDto updateStock(String sku, int delta) {
        String cleanSku = sku.trim();
        List<CatalogPartJpaEntity> matching = catalogPartRepository.findByVtexSku(cleanSku);
        if (matching.isEmpty()) {
            matching = catalogPartRepository.findAll().stream()
                    .filter(p -> p.getVtexSku().equalsIgnoreCase(cleanSku))
                    .toList();
        }
        if (matching.isEmpty()) {
            throw new ResourceNotFoundException("Peça não encontrada no catálogo para o SKU: " + cleanSku);
        }

        CatalogPartJpaEntity part = matching.getFirst();
        int newStock = serviceOrderPolicy.calculateUpdatedStock(part.getStockQuantity(), delta);
        part.setStockQuantity(newStock);
        part.setUpdatedAt(Instant.now());
        CatalogPartJpaEntity saved = catalogPartRepository.save(part);

        return toStockDto(saved);
    }

    private ServiceOrderResponseDto toOrderDto(ServiceOrderJpaEntity order) {
        String plate = "MOT-OPS";
        String model = "Mottu Sport 110i";

        Optional<MotorcycleJpaEntity> motoOpt = motorcycleRepository.findById(order.getMotorcycleId());
        if (motoOpt.isPresent()) {
            MotorcycleJpaEntity moto = motoOpt.get();
            plate = moto.getPlate();
            model = formatModelName(moto.getModel());
        }

        return new ServiceOrderResponseDto(
                order.getId(),
                order.getCode(),
                plate,
                model,
                order.getBayLabel(),
                order.getServiceTitle(),
                order.getOriginLabel(),
                order.getSlaMinutes(),
                order.getStatus()
        );
    }

    private StockItemResponseDto toStockDto(CatalogPartJpaEntity part) {
        return new StockItemResponseDto(
                part.getId(),
                part.getVtexSku(),
                part.getPartName(),
                formatModelName(part.getMotorcycleModel()),
                part.getAisleBin() != null ? part.getAisleBin() : "Corredor A • Gaveta 01",
                part.getStockQuantity(),
                part.getMinStockQuantity(),
                part.getUnitPriceBrl()
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

