package com.manublock.backend.models;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "transports")
public class Transport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String trackingNumber;

    @ManyToOne
    @JoinColumn(name = "distributor_id", nullable = false)
    private Users distributor;

    @ManyToOne
    @JoinColumn(name = "source_id", nullable = false)
    private Users source; // Either supplier or manufacturer

    @ManyToOne
    @JoinColumn(name = "destination_id", nullable = false)
    private Users destination; // Either manufacturer or customer

    @ManyToOne
    @JoinColumn(name = "material_request_id")
    private MaterialRequest materialRequest; // If transporting materials

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order; // If transporting finished products

    @ManyToOne
    @JoinColumn(name = "supply_chain_id", nullable = false)
    private Chains supplyChain;

    @Column(nullable = false)
    private String type; // Material Transport, Product Delivery

    @Column(nullable = false)
    private String status; // Scheduled, In Transit, Delivered, Confirmed, Cancelled

    @Column
    private String pickupLocation;

    @Column
    private String deliveryLocation;

    @Column
    private Date scheduledPickupDate;

    @Column
    private Date actualPickupDate;

    @Column
    private Date scheduledDeliveryDate;

    @Column
    private Date actualDeliveryDate;

    @Column
    private String notes;

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

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public Users getDistributor() {
        return distributor;
    }

    public void setDistributor(Users distributor) {
        this.distributor = distributor;
    }

    public Users getSource() {
        return source;
    }

    public void setSource(Users source) {
        this.source = source;
    }

    public Users getDestination() {
        return destination;
    }

    public void setDestination(Users destination) {
        this.destination = destination;
    }

    public MaterialRequest getMaterialRequest() {
        return materialRequest;
    }

    public void setMaterialRequest(MaterialRequest materialRequest) {
        this.materialRequest = materialRequest;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Chains getSupplyChain() {
        return supplyChain;
    }

    public void setSupplyChain(Chains supplyChain) {
        this.supplyChain = supplyChain;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public Date getScheduledPickupDate() {
        return scheduledPickupDate;
    }

    public void setScheduledPickupDate(Date scheduledPickupDate) {
        this.scheduledPickupDate = scheduledPickupDate;
    }

    public Date getActualPickupDate() {
        return actualPickupDate;
    }

    public void setActualPickupDate(Date actualPickupDate) {
        this.actualPickupDate = actualPickupDate;
    }

    public Date getScheduledDeliveryDate() {
        return scheduledDeliveryDate;
    }

    public void setScheduledDeliveryDate(Date scheduledDeliveryDate) {
        this.scheduledDeliveryDate = scheduledDeliveryDate;
    }

    public Date getActualDeliveryDate() {
        return actualDeliveryDate;
    }

    public void setActualDeliveryDate(Date actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
