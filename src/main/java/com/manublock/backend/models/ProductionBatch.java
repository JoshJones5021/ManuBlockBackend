package com.manublock.backend.models;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "production_batches")
public class ProductionBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String batchNumber;

    @ManyToOne
    @JoinColumn(name = "manufacturer_id", nullable = false)
    private Users manufacturer;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order relatedOrder; // Optional - may be related to a specific order

    @ManyToOne
    @JoinColumn(name = "supply_chain_id", nullable = false)
    private Chains supplyChain;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false)
    private String status; // Planned, In Production, In QC, Completed, Rejected

    @Column
    private Date startDate;

    @Column
    private Date completionDate;

    @Column
    private String quality; // QC results if applicable

    @Column
    private String notes;

    @Column
    private Long blockchainItemId; // Reference to blockchain item for the produced batch

    @ManyToMany
    @JoinTable(
            name = "production_batch_materials",
            joinColumns = @JoinColumn(name = "batch_id"),
            inverseJoinColumns = @JoinColumn(name = "material_id")
    )
    private List<Material> usedMaterials;

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

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Users getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Users manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Order getRelatedOrder() {
        return relatedOrder;
    }

    public void setRelatedOrder(Order relatedOrder) {
        this.relatedOrder = relatedOrder;
    }

    public Chains getSupplyChain() {
        return supplyChain;
    }

    public void setSupplyChain(Chains supplyChain) {
        this.supplyChain = supplyChain;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getBlockchainItemId() {
        return blockchainItemId;
    }

    public void setBlockchainItemId(Long blockchainItemId) {
        this.blockchainItemId = blockchainItemId;
    }

    public List<Material> getUsedMaterials() {
        return usedMaterials;
    }

    public void setUsedMaterials(List<Material> usedMaterials) {
        this.usedMaterials = usedMaterials;
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