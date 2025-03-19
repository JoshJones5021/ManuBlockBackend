package com.manublock.backend.models;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "material_requests")
public class MaterialRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String requestNumber;

    @ManyToOne
    @JoinColumn(name = "manufacturer_id", nullable = false)
    private Users manufacturer;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Users supplier;

    @ManyToOne
    @JoinColumn(name = "supply_chain_id", nullable = false)
    private Chains supplyChain;

    @OneToMany(mappedBy = "materialRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MaterialRequestItem> items;

    @Column(nullable = false)
    private String status; // Requested, Approved, Allocated, Ready for Pickup, In Transit, Delivered, Completed, Rejected

    @Column
    private String notes;

    @Column
    private Date requestedDeliveryDate;

    @Column
    private Date actualDeliveryDate;

    @Column
    private String blockchainTxHash;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public Users getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Users manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Users getSupplier() {
        return supplier;
    }

    public void setSupplier(Users supplier) {
        this.supplier = supplier;
    }

    public Chains getSupplyChain() {
        return supplyChain;
    }

    public void setSupplyChain(Chains supplyChain) {
        this.supplyChain = supplyChain;
    }

    public List<MaterialRequestItem> getItems() {
        return items;
    }

    public void setItems(List<MaterialRequestItem> items) {
        this.items = items;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getRequestedDeliveryDate() {
        return requestedDeliveryDate;
    }

    public void setRequestedDeliveryDate(Date requestedDeliveryDate) {
        this.requestedDeliveryDate = requestedDeliveryDate;
    }

    public Date getActualDeliveryDate() {
        return actualDeliveryDate;
    }

    public void setActualDeliveryDate(Date actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }

    public String getBlockchainTxHash() {
        return blockchainTxHash;
    }

    public void setBlockchainTxHash(String blockchainTxHash) {
        this.blockchainTxHash = blockchainTxHash;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}