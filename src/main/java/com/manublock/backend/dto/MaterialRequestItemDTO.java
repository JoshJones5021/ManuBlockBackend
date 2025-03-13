package com.manublock.backend.dto;

import com.manublock.backend.dto.MaterialDTO;
import com.manublock.backend.models.MaterialRequestItem;

class MaterialRequestItemDTO {
    private Long id;
    private MaterialDTO material;
    private Long requestedQuantity;
    private Long approvedQuantity;
    private Long allocatedQuantity;
    private String status;
    private Long blockchainItemId;

    public MaterialRequestItemDTO(MaterialRequestItem item) {
        this.id = item.getId();
        this.material = new MaterialDTO(item.getMaterial());
        this.requestedQuantity = item.getRequestedQuantity();
        this.approvedQuantity = item.getApprovedQuantity();
        this.allocatedQuantity = item.getAllocatedQuantity();
        this.status = item.getStatus();
        this.blockchainItemId = item.getBlockchainItemId();
    }

    // Getters
    public Long getId() { return id; }
    public MaterialDTO getMaterial() { return material; }
    public Long getRequestedQuantity() { return requestedQuantity; }
    public Long getApprovedQuantity() { return approvedQuantity; }
    public Long getAllocatedQuantity() { return allocatedQuantity; }
    public String getStatus() { return status; }
    public Long getBlockchainItemId() { return blockchainItemId; }
}