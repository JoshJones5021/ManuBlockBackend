package com.manublock.backend.dto;

public class MaterialRequestItemCreateDTO {
    private Long materialId;
    private Long quantity;

    // Getters and setters
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
}