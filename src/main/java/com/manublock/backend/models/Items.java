package com.manublock.backend.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "items")
public class Items {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String itemType;

    @Column(nullable = false)
    private Long quantity;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonBackReference(value = "user-items")
    private Users owner;

    @ManyToOne
    @JoinColumn(name = "supply_chain_id", nullable = false)
    @JsonBackReference(value = "chain-items")
    private Chains supplyChain;

    @Column(nullable = false)
    private String status;

    @ElementCollection
    @CollectionTable(name = "item_parents", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "parent_id")
    private List<Long> parentItemIds;

    @Column
    private String blockchainTxHash;

    @Column
    private String blockchainStatus;

    @Column
    private String notes;

    @Column
    private String pickupAddress;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Users getOwner() {
        return owner;
    }

    public void setOwner(Users owner) {
        this.owner = owner;
    }

    public Chains getSupplyChain() {
        return supplyChain;
    }

    public void setSupplyChain(Chains supplyChain) {
        this.supplyChain = supplyChain;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Long> getParentItemIds() {
        return parentItemIds;
    }

    public void setParentItemIds(List<Long> parentItemIds) {
        this.parentItemIds = parentItemIds;
    }

    public String getBlockchainTxHash() {
        return blockchainTxHash;
    }

    public void setBlockchainTxHash(String blockchainTxHash) {
        this.blockchainTxHash = blockchainTxHash;
    }

    public String getBlockchainStatus() {
        return blockchainStatus;
    }

    public void setBlockchainStatus(String blockchainStatus) {
        this.blockchainStatus = blockchainStatus;
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

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}