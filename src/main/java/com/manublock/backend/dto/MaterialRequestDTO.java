package com.manublock.backend.dto;

import com.manublock.backend.models.MaterialRequest;
import com.manublock.backend.models.MaterialRequestItem;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MaterialRequestDTO {
    private Long id;
    private String requestNumber;
    private String status;
    private ManufacturerDTO manufacturer;
    private Long supplierId;
    private Long supplyChainId;
    private Date createdAt;
    private Date updatedAt;
    private Date requestedDeliveryDate;
    private List<MaterialRequestItemDTO> items;

    public MaterialRequestDTO(MaterialRequest request) {
        this.id = request.getId();
        this.requestNumber = request.getRequestNumber();
        this.status = request.getStatus();
        this.manufacturer = new ManufacturerDTO(request.getManufacturer());
        this.supplierId = request.getSupplier().getId();
        this.supplyChainId = request.getSupplyChain().getId();
        this.createdAt = request.getCreatedAt();
        this.updatedAt = request.getUpdatedAt();
        this.requestedDeliveryDate = request.getRequestedDeliveryDate();

        // Create DTOs for items to avoid circular references
        this.items = request.getItems().stream()
                .map(MaterialRequestItemDTO::new)
                .collect(Collectors.toList());
    }

    // Getters
    public Long getId() { return id; }
    public String getRequestNumber() { return requestNumber; }
    public String getStatus() { return status; }
    public ManufacturerDTO getManufacturer() { return manufacturer; }
    public Long getSupplierId() { return supplierId; }
    public Long getSupplyChainId() { return supplyChainId; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public Date getRequestedDeliveryDate() { return requestedDeliveryDate; }
    public List<MaterialRequestItemDTO> getItems() { return items; }
}