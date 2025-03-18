package com.manublock.backend.dto;

import com.manublock.backend.models.Material;

public class MaterialDTO {
    private Long id;
    private String name;
    private String description;
    private String unit;
    private Long quantity;
    private Long blockchainItemId;
    private String itemType;

    public MaterialDTO(Material material) {
        this.id = material.getId();
        this.name = material.getName();
        this.description = material.getDescription();
        this.unit = material.getUnit();
        this.quantity = material.getQuantity();
        this.blockchainItemId = material.getBlockchainItemId();
    }

    public MaterialDTO() {
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Long getQuantity() { return quantity; }
    public void setQuantity(Long quantity) { this.quantity = quantity; }

    public Long getBlockchainItemId() { return blockchainItemId; }
    public void setBlockchainItemId(Long blockchainItemId) { this.blockchainItemId = blockchainItemId; }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
}