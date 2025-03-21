package com.manublock.backend.dto;

import com.manublock.backend.models.Transport;
import java.util.Date;

public class TransportResponseDTO {
    private Long id;
    private String trackingNumber;
    private String type;
    private String status;
    private Long distributorId;
    private String distributorName;
    private Long sourceId;
    private String sourceName;
    private Long destinationId;
    private String destinationName;
    private Long materialRequestId;
    private String materialRequestNumber;
    private Long orderId;
    private String orderNumber;
    private Long supplyChainId;
    private String supplyChainName;
    private Date scheduledPickupDate;
    private Date actualPickupDate;
    private Date scheduledDeliveryDate;
    private Date actualDeliveryDate;
    private String blockchainTxHash;
    private Date createdAt;
    private Date updatedAt;

    public TransportResponseDTO(Transport transport) {
        this.id = transport.getId();
        this.trackingNumber = transport.getTrackingNumber();
        this.type = transport.getType();
        this.status = transport.getStatus();

        // Distributor
        if (transport.getDistributor() != null) {
            this.distributorId = transport.getDistributor().getId();
            this.distributorName = transport.getDistributor().getUsername();
        }

        // Source
        if (transport.getSource() != null) {
            this.sourceId = transport.getSource().getId();
            this.sourceName = transport.getSource().getUsername();
        }

        // Destination
        if (transport.getDestination() != null) {
            this.destinationId = transport.getDestination().getId();
            this.destinationName = transport.getDestination().getUsername();
        }

        // Material Request
        if (transport.getMaterialRequest() != null) {
            this.materialRequestId = transport.getMaterialRequest().getId();
            this.materialRequestNumber = transport.getMaterialRequest().getRequestNumber();
        }

        // Order
        if (transport.getOrder() != null) {
            this.orderId = transport.getOrder().getId();
            this.orderNumber = transport.getOrder().getOrderNumber();
        }

        // Supply Chain
        if (transport.getSupplyChain() != null) {
            this.supplyChainId = transport.getSupplyChain().getId();
            this.supplyChainName = transport.getSupplyChain().getName();
        }

        // Other fields
        this.scheduledPickupDate = transport.getScheduledPickupDate();
        this.actualPickupDate = transport.getActualPickupDate();
        this.scheduledDeliveryDate = transport.getScheduledDeliveryDate();
        this.actualDeliveryDate = transport.getActualDeliveryDate();
        this.blockchainTxHash = transport.getBlockchainTxHash();
        this.createdAt = transport.getCreatedAt();
        this.updatedAt = transport.getUpdatedAt();
    }

    // Getters
    public Long getId() { return id; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public Long getDistributorId() { return distributorId; }
    public String getDistributorName() { return distributorName; }
    public Long getSourceId() { return sourceId; }
    public String getSourceName() { return sourceName; }
    public Long getDestinationId() { return destinationId; }
    public String getDestinationName() { return destinationName; }
    public Long getMaterialRequestId() { return materialRequestId; }
    public String getMaterialRequestNumber() { return materialRequestNumber; }
    public Long getOrderId() { return orderId; }
    public String getOrderNumber() { return orderNumber; }
    public Long getSupplyChainId() { return supplyChainId; }
    public String getSupplyChainName() { return supplyChainName; }
    public Date getScheduledPickupDate() { return scheduledPickupDate; }
    public Date getActualPickupDate() { return actualPickupDate; }
    public Date getScheduledDeliveryDate() { return scheduledDeliveryDate; }
    public Date getActualDeliveryDate() { return actualDeliveryDate; }
    public String getBlockchainTxHash() { return blockchainTxHash; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
}