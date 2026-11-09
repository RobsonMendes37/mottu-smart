package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import com.mottu.ops.domain.enums.MotorcycleModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_parts")
public class CatalogPartJpaEntity {

    @Id
    private UUID id;

    @Column(name = "vtex_sku", nullable = false, length = 60)
    private String vtexSku;

    @Enumerated(EnumType.STRING)
    @Column(name = "motorcycle_model", nullable = false, length = 30)
    private MotorcycleModel motorcycleModel;

    @Column(name = "part_name", nullable = false, length = 180)
    private String partName;

    @Column(name = "category", nullable = false, length = 80)
    private String category;

    @Column(name = "unit_price_brl", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceBrl;

    @Column(name = "is_natural_wear", nullable = false)
    private boolean isNaturalWear;

    @Column(name = "price_overridden", nullable = false)
    private boolean priceOverridden;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 18;

    @Column(name = "min_stock_quantity")
    private Integer minStockQuantity = 8;

    @Column(name = "aisle_bin", length = 60)
    private String aisleBin = "Corredor A • Gaveta 01";

    public CatalogPartJpaEntity() {
    }

    public CatalogPartJpaEntity(
            UUID id,
            String vtexSku,
            MotorcycleModel motorcycleModel,
            String partName,
            String category,
            BigDecimal unitPriceBrl,
            boolean isNaturalWear,
            boolean priceOverridden,
            String imageUrl,
            Instant updatedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.vtexSku = vtexSku;
        this.motorcycleModel = motorcycleModel;
        this.partName = partName;
        this.category = category;
        this.unitPriceBrl = unitPriceBrl;
        this.isNaturalWear = isNaturalWear;
        this.priceOverridden = priceOverridden;
        this.imageUrl = imageUrl;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getVtexSku() {
        return vtexSku;
    }

    public void setVtexSku(String vtexSku) {
        this.vtexSku = vtexSku;
    }

    public MotorcycleModel getMotorcycleModel() {
        return motorcycleModel;
    }

    public void setMotorcycleModel(MotorcycleModel motorcycleModel) {
        this.motorcycleModel = motorcycleModel;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getUnitPriceBrl() {
        return unitPriceBrl;
    }

    public void setUnitPriceBrl(BigDecimal unitPriceBrl) {
        this.unitPriceBrl = unitPriceBrl;
    }

    public boolean isNaturalWear() {
        return isNaturalWear;
    }

    public void setNaturalWear(boolean naturalWear) {
        isNaturalWear = naturalWear;
    }

    public boolean isPriceOverridden() {
        return priceOverridden;
    }

    public void setPriceOverridden(boolean priceOverridden) {
        this.priceOverridden = priceOverridden;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getStockQuantity() {
        return stockQuantity != null ? stockQuantity : 18;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity != null ? stockQuantity : 18;
    }

    public int getMinStockQuantity() {
        return minStockQuantity != null ? minStockQuantity : 8;
    }

    public void setMinStockQuantity(Integer minStockQuantity) {
        this.minStockQuantity = minStockQuantity != null ? minStockQuantity : 8;
    }

    public String getAisleBin() {
        return (aisleBin != null && !aisleBin.isBlank()) ? aisleBin : "Corredor A • Gaveta 01";
    }

    public void setAisleBin(String aisleBin) {
        this.aisleBin = (aisleBin != null && !aisleBin.isBlank()) ? aisleBin : "Corredor A • Gaveta 01";
    }
}
