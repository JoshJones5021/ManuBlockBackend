package com.manublock.backend.dto;

import com.manublock.backend.models.Material;

class MaterialDTO {
    private Long id;
    private String name;
    private String description;
    private String unit;
    private Long quantity;

    public MaterialDTO(Material material) {
        this.id = material.getId();
        this.name = material.getName();
        this.description = material.getDescription();
        this.unit = material.getUnit();
        this.quantity = material.getQuantity();
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getUnit() { return unit; }
    public Long getQuantity() { return quantity; }
}