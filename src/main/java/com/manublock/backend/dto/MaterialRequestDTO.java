package com.manublock.backend.dto;

import com.manublock.backend.models.Chains;
import com.manublock.backend.models.MaterialRequest;
import com.manublock.backend.models.Users;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MaterialRequestDTO {
    private Long id;
    private String requestNumber;
    private String status;
    private ManufacturerDTO manufacturer;
    private SupplierDTO supplier; // Changed from supplierId to a DTO
    private SupplyChainDTO supplyChain; // Changed from supplyChainId to a DTO
    private Date createdAt;
    private Date updatedAt;
    private Date requestedDeliveryDate;
    private List<MaterialRequestItemDTO> items;

    public MaterialRequestDTO(MaterialRequest request) {
        this.id = request.getId();
        this.requestNumber = request.getRequestNumber();
        this.status = request.getStatus();
        this.manufacturer = new ManufacturerDTO(request.getManufacturer());
        this.supplier = new SupplierDTO(request.getSupplier());
        this.supplyChain = new SupplyChainDTO(request.getSupplyChain());
        this.createdAt = request.getCreatedAt();
        this.updatedAt = request.getUpdatedAt();
        this.requestedDeliveryDate = request.getRequestedDeliveryDate();

        // Create DTOs for items to avoid circular references
        this.items = request.getItems().stream()
                .map(MaterialRequestItemDTO::new)
                .collect(Collectors.toList());
    }

    // Nested static DTO classes to eliminate circular references
    public static class SupplierDTO {
        private Long id;
        private String username;

        public SupplierDTO(Users supplier) {
            this.id = supplier.getId();
            this.username = supplier.getUsername();
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
    }

    public static class SupplyChainDTO {
        private Long id;
        private String name;

        public SupplyChainDTO(Chains supplyChain) {
            this.id = supplyChain.getId();
            this.name = supplyChain.getName();
        }

        public Long getId() { return id; }
        public String getName() { return name; }
    }

    // Getters
    public Long getId() { return id; }
    public String getRequestNumber() { return requestNumber; }
    public String getStatus() { return status; }
    public ManufacturerDTO getManufacturer() { return manufacturer; }
    public SupplierDTO getSupplier() { return supplier; }
    public SupplyChainDTO getSupplyChain() { return supplyChain; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public Date getRequestedDeliveryDate() { return requestedDeliveryDate; }
    public List<MaterialRequestItemDTO> getItems() { return items; }
}