package com.manublock.backend.dto;

import java.util.Date;
import java.util.List;

public class MaterialRequestCreateDTO {
    private Long manufacturerId;
    private Long supplierId;
    private Long supplyChainId;
    private List<MaterialRequestItemCreateDTO> items;
    private Date requestedDeliveryDate;
    private String notes;
    private String status;

    // Getters and setters
    public Long getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(Long manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getSupplyChainId() {
        return supplyChainId;
    }

    public void setSupplyChainId(Long supplyChainId) {
        this.supplyChainId = supplyChainId;
    }

    public List<MaterialRequestItemCreateDTO> getItems() {
        return items;
    }

    public void setItems(List<MaterialRequestItemCreateDTO> items) {
        this.items = items;
    }

    public Date getRequestedDeliveryDate() {
        return requestedDeliveryDate;
    }

    public void setRequestedDeliveryDate(Date requestedDeliveryDate) {
        this.requestedDeliveryDate = requestedDeliveryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}