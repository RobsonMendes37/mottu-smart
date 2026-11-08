package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.CatalogPartResponseDto;
import com.mottu.ops.application.dto.PageResponse;
import com.mottu.ops.application.dto.SyncCatalogResponseDto;
import com.mottu.ops.application.dto.UpdateCatalogPartRequest;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.in.ManagerCatalogUseCase;
import com.mottu.ops.application.port.out.CatalogSyncPort;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.CatalogPartJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.SystemSettingsJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataCatalogPartRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataSystemSettingsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ManagerCatalogService implements ManagerCatalogUseCase {

    private final SpringDataCatalogPartRepository catalogPartRepository;
    private final SpringDataSystemSettingsRepository settingsRepository;
    private final CatalogSyncPort catalogSyncPort;

    public ManagerCatalogService(
            SpringDataCatalogPartRepository catalogPartRepository,
            SpringDataSystemSettingsRepository settingsRepository,
            CatalogSyncPort catalogSyncPort
    ) {
        this.catalogPartRepository = catalogPartRepository;
        this.settingsRepository = settingsRepository;
        this.catalogSyncPort = catalogSyncPort;
    }

    @Override
    @Transactional
    public SyncCatalogResponseDto syncMottuStore() {
        SyncCatalogResponseDto result = catalogSyncPort.syncCatalog();

        SystemSettingsJpaEntity syncSetting = settingsRepository.findBySettingKey("LAST_CATALOG_SYNC_AT")
                .orElseGet(() -> new SystemSettingsJpaEntity("LAST_CATALOG_SYNC_AT", result.syncedAt().toString(), result.syncedAt()));
        syncSetting.setSettingValue(result.syncedAt().toString());
        syncSetting.setUpdatedAt(result.syncedAt());
        settingsRepository.save(syncSetting);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CatalogPartResponseDto> getParts(MotorcycleModel model, String search, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<CatalogPartJpaEntity> page;
        if (model != null && cleanSearch != null) {
            page = catalogPartRepository.findByModelAndSearch(model, cleanSearch, pageable);
        } else if (model != null) {
            page = catalogPartRepository.findByMotorcycleModel(model, pageable);
        } else if (cleanSearch != null) {
            page = catalogPartRepository.findBySearch(cleanSearch, pageable);
        } else {
            page = catalogPartRepository.findAll(pageable);
        }

        return new PageResponse<>(
                page.getContent().stream().map(this::toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional
    public CatalogPartResponseDto updatePart(UUID id, UpdateCatalogPartRequest request) {
        CatalogPartJpaEntity part = catalogPartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Peça do catálogo não encontrada: " + id));

        if (request.unitPriceBrl() != null) {
            part.setUnitPriceBrl(request.unitPriceBrl());
            part.setPriceOverridden(true);
        }
        if (request.isNaturalWear() != null) {
            part.setNaturalWear(request.isNaturalWear());
        }
        part.setUpdatedAt(Instant.now());

        CatalogPartJpaEntity saved = catalogPartRepository.save(part);
        return toDto(saved);
    }

    private CatalogPartResponseDto toDto(CatalogPartJpaEntity entity) {
        return new CatalogPartResponseDto(
                entity.getId(),
                entity.getVtexSku(),
                entity.getMotorcycleModel(),
                entity.getPartName(),
                entity.getCategory(),
                entity.getUnitPriceBrl(),
                entity.isNaturalWear(),
                entity.isPriceOverridden(),
                entity.getImageUrl(),
                entity.getUpdatedAt()
        );
    }
}
