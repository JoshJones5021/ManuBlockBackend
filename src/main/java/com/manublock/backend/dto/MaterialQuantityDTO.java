package com.manublock.backend.dto;

public class MaterialQuantityDTO {
    private Long materialId;
    private Long quantity;
    private String unit;

    public MaterialQuantityDTO() {
    }

    public MaterialQuantityDTO(Long materialId, Long quantity, String unit) {
        this.materialId = materialId;
        this.quantity = quantity;
        this.unit = unit;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
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