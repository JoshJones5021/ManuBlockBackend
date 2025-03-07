package com.manublock.backend.models;

import jakarta.persistence.*;

@Entity
@Table(name = "material_request_items")
public class MaterialRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "material_request_id", nullable = false)
    private MaterialRequest materialRequest;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false)
    private Long requestedQuantity;

    @Column
    private Long approvedQuantity;

    @Column
    private Long allocatedQuantity;

    @Column
    private Long deliveredQuantity;

    @Column
    private String status; // Requested, Approved, Allocated, Delivered, Rejected

    @Column
    private Long blockchainItemId; // Reference to blockchain item once allocated

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MaterialRequest getMaterialRequest() {
        return materialRequest;
    }

    public void setMaterialRequest(MaterialRequest materialRequest) {
        this.materialRequest = materialRequest;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Long getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Long requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Long getApprovedQuantity() {
        return approvedQuantity;
    }

    public void setApprovedQuantity(Long approvedQuantity) {
        this.approvedQuantity = approvedQuantity;
    }

    public Long getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(Long allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
    }

    public Long getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public void setDeliveredQuantity(Long deliveredQuantity) {
        this.deliveredQuantity = deliveredQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getBlockchainItemId() {
        return blockchainItemId;
    }

    public void setBlockchainItemId(Long blockchainItemId) {
        this.blockchainItemId = blockchainItemId;
    }
}

