package com.manublock.backend.dto;

import com.manublock.backend.models.Material;
import com.manublock.backend.models.MaterialRequestItem;

public class MaterialRequestItemDTO {
    private Long id;
    private MaterialSimpleDTO material;
    private Long requestedQuantity;
    private Long approvedQuantity;
    private Long allocatedQuantity;
    private String status;
    private Long blockchainItemId;

    public MaterialRequestItemDTO(MaterialRequestItem item) {
        this.id = item.getId();
        // Use a simplified material DTO to avoid recursion
        this.material = new MaterialSimpleDTO(item.getMaterial());
        this.requestedQuantity = item.getRequestedQuantity();
        this.approvedQuantity = item.getApprovedQuantity();
        this.allocatedQuantity = item.getAllocatedQuantity();
        this.status = item.getStatus();
        this.blockchainItemId = item.getBlockchainItemId();
    }

    // Static inner class for simplified material representation
    public static class MaterialSimpleDTO {
        private Long id;
        private String name;
        private String description;
        private String unit;
        private String specifications;

        public MaterialSimpleDTO(Material material) {
            this.id = material.getId();
            this.name = material.getName();
            this.description = material.getDescription();
            this.unit = material.getUnit();
            this.specifications = material.getSpecifications();
        }

        // Getters
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getUnit() { return unit; }
        public String getSpecifications() { return specifications; }
    }

    // Getters
    public Long getId() { return id; }
    public MaterialSimpleDTO getMaterial() { return material; }
    public Long getRequestedQuantity() { return requestedQuantity; }
    public Long getApprovedQuantity() { return approvedQuantity; }
    public Long getAllocatedQuantity() { return allocatedQuantity; }
    public String getStatus() { return status; }
    public Long getBlockchainItemId() { return blockchainItemId; }
}