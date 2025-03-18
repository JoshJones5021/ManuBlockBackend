package com.manublock.backend.dto;

import com.manublock.backend.models.Product;
import com.manublock.backend.models.ProductMaterialQuantity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private String specifications;
    private String sku;
    private BigDecimal price;
    private Long availableQuantity;
    private Long manufacturerId;
    private String manufacturerName;
    private String productionBatch;
    private boolean active;
    private Long blockchainItemId;
    private Date createdAt;
    private Date updatedAt;
    private List<MaterialWithQuantityDTO> materials = new ArrayList<>();

    // Static method to convert from entity
    public static ProductDTO fromEntity(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setSpecifications(product.getSpecifications());
        dto.setSku(product.getSku());
        dto.setPrice(product.getPrice());
        dto.setAvailableQuantity(product.getAvailableQuantity());

        if (product.getManufacturer() != null) {
            dto.setManufacturerId(product.getManufacturer().getId());
            dto.setManufacturerName(product.getManufacturer().getUsername());
        }

        dto.setProductionBatch(product.getProductionBatch());
        dto.setActive(product.isActive());
        dto.setBlockchainItemId(product.getBlockchainItemId());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        // Convert material quantities
        if (product.getMaterialQuantities() != null) {
            for (ProductMaterialQuantity pmq : product.getMaterialQuantities()) {
                MaterialWithQuantityDTO materialDTO = new MaterialWithQuantityDTO();
                materialDTO.setMaterialId(pmq.getMaterial().getId());
                materialDTO.setMaterialName(pmq.getMaterial().getName());
                materialDTO.setQuantity(pmq.getQuantity());
                // Get the unit from the material if available
                if (pmq.getMaterial() != null && pmq.getMaterial().getUnit() != null) {
                    materialDTO.setUnit(pmq.getMaterial().getUnit());
                } else {
                    materialDTO.setUnit(""); // Default to empty string if unit is null
                }
                dto.getMaterials().add(materialDTO);
            }
        }

        return dto;
    }

    // Inner DTO class for materials with quantities
    public static class MaterialWithQuantityDTO {
        private Long materialId;
        private String materialName;
        private Long quantity;
        private String unit;

        // Getters and setters
        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public String getMaterialName() {
            return materialName;
        }

        public void setMaterialName(String materialName) {
            this.materialName = materialName;
        }

        public Long getQuantity() {
            return quantity;
        }

        public void setQuantity(Long quantity) {
            this.quantity = quantity;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }

    // All the getters and setters for ProductDTO
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Long availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Long getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(Long manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    public String getProductionBatch() {
        return productionBatch;
    }

    public void setProductionBatch(String productionBatch) {
        this.productionBatch = productionBatch;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getBlockchainItemId() {
        return blockchainItemId;
    }

    public void setBlockchainItemId(Long blockchainItemId) {
        this.blockchainItemId = blockchainItemId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<MaterialWithQuantityDTO> getMaterials() {
        return materials;
    }

    public void setMaterials(List<MaterialWithQuantityDTO> materials) {
        this.materials = materials;
    }
}