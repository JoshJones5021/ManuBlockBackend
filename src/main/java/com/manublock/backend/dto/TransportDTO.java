package com.manublock.backend.dto;

import com.manublock.backend.models.MaterialRequest;
import com.manublock.backend.models.Order;
import com.manublock.backend.models.Transport;

import java.util.Date;

public class TransportDTO {
    private Long id;
    private String trackingNumber;
    private String type;
    private String status;
    private UserResponseDTO distributor;
    private UserResponseDTO source;
    private UserResponseDTO destination;
    private Long materialRequestId;
    private String materialRequestNumber;
    private Long orderId;
    private String orderNumber;
    private Long supplyChainId;
    private Date scheduledPickupDate;
    private Date actualPickupDate;
    private Date scheduledDeliveryDate;
    private Date actualDeliveryDate;
    private String pickupLocation;
    private String deliveryLocation;
    private String notes;
    private String blockchainTxHash;
    private Date createdAt;
    private Date updatedAt;

    public TransportDTO(Transport transport) {
        this.id = transport.getId();
        this.trackingNumber = transport.getTrackingNumber();
        this.type = transport.getType();
        this.status = transport.getStatus();

        // User references
        this.distributor = transport.getDistributor() != null ?
                new UserResponseDTO(transport.getDistributor()) : null;
        this.source = transport.getSource() != null ?
                new UserResponseDTO(transport.getSource()) : null;
        this.destination = transport.getDestination() != null ?
                new UserResponseDTO(transport.getDestination()) : null;

        // Related entities (just IDs and basic info, not full objects)
        if (transport.getMaterialRequest() != null) {
            MaterialRequest request = transport.getMaterialRequest();
            this.materialRequestId = request.getId();
            this.materialRequestNumber = request.getRequestNumber();
        }

        if (transport.getOrder() != null) {
            Order order = transport.getOrder();
            this.orderId = order.getId();
            this.orderNumber = order.getOrderNumber();
        }

        this.supplyChainId = transport.getSupplyChain() != null ?
                transport.getSupplyChain().getId() : null;

        // Dates and other fields
        this.scheduledPickupDate = transport.getScheduledPickupDate();
        this.actualPickupDate = transport.getActualPickupDate();
        this.scheduledDeliveryDate = transport.getScheduledDeliveryDate();
        this.actualDeliveryDate = transport.getActualDeliveryDate();
        this.pickupLocation = transport.getPickupLocation();
        this.deliveryLocation = transport.getDeliveryLocation();
        this.notes = transport.getNotes();
        this.blockchainTxHash = transport.getBlockchainTxHash();
        this.createdAt = transport.getCreatedAt();
        this.updatedAt = transport.getUpdatedAt();
    }

    // Getters
    public Long getId() { return id; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public UserResponseDTO getDistributor() { return distributor; }
    public UserResponseDTO getSource() { return source; }
    public UserResponseDTO getDestination() { return destination; }
    public Long getMaterialRequestId() { return materialRequestId; }
    public String getMaterialRequestNumber() { return materialRequestNumber; }
    public Long getOrderId() { return orderId; }
    public String getOrderNumber() { return orderNumber; }
    public Long getSupplyChainId() { return supplyChainId; }
    public Date getScheduledPickupDate() { return scheduledPickupDate; }
    public Date getActualPickupDate() { return actualPickupDate; }
    public Date getScheduledDeliveryDate() { return scheduledDeliveryDate; }
    public Date getActualDeliveryDate() { return actualDeliveryDate; }
    public String getPickupLocation() { return pickupLocation; }
    public String getDeliveryLocation() { return deliveryLocation; }
    public String getNotes() { return notes; }
    public String getBlockchainTxHash() { return blockchainTxHash; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
}