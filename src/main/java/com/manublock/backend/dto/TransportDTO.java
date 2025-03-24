package com.manublock.backend.dto;

import com.manublock.backend.models.*;

import java.util.Date;

public class TransportDTO {
    private Long id;
    private String trackingNumber;
    private String type;
    private String status;
    private UserReferenceDTO distributor;
    private UserReferenceDTO source;
    private UserReferenceDTO destination;
    private MaterialRequestReferenceDTO materialRequest;
    private OrderReferenceDTO order;
    private SupplyChainReferenceDTO supplyChain;
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
    private RecycledItemReferenceDTO recycledItem; // Added for recycling

    public TransportDTO(Transport transport) {
        this.id = transport.getId();
        this.trackingNumber = transport.getTrackingNumber();
        this.type = transport.getType();
        this.status = transport.getStatus();

        // User references
        this.distributor = transport.getDistributor() != null ?
                new UserReferenceDTO(transport.getDistributor()) : null;
        this.source = transport.getSource() != null ?
                new UserReferenceDTO(transport.getSource()) : null;
        this.destination = transport.getDestination() != null ?
                new UserReferenceDTO(transport.getDestination()) : null;

        // Related entities (just basic references, not full objects)
        if (transport.getMaterialRequest() != null) {
            this.materialRequest = new MaterialRequestReferenceDTO(transport.getMaterialRequest());
        }

        if (transport.getOrder() != null) {
            this.order = new OrderReferenceDTO(transport.getOrder());
        }

        // Supply chain reference
        this.supplyChain = transport.getSupplyChain() != null ?
                new SupplyChainReferenceDTO(transport.getSupplyChain()) : null;

        // Recycled item reference
        this.recycledItem = transport.getRecycledItem() != null ?
                new RecycledItemReferenceDTO(transport.getRecycledItem()) : null;

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

    // Simplified reference DTOs to prevent recursion
    public static class UserReferenceDTO {
        private Long id;
        private String username;
        private String role;

        public UserReferenceDTO(Users user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.role = user.getRole().name();
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }

    public static class MaterialRequestReferenceDTO {
        private Long id;
        private String requestNumber;

        public MaterialRequestReferenceDTO(MaterialRequest request) {
            this.id = request.getId();
            this.requestNumber = request.getRequestNumber();
        }

        public Long getId() { return id; }
        public String getRequestNumber() { return requestNumber; }
    }

    public static class OrderReferenceDTO {
        private Long id;
        private String orderNumber;
        private String status;

        public OrderReferenceDTO(Order order) {
            this.id = order.getId();
            this.orderNumber = order.getOrderNumber();
            this.status = order.getStatus();
        }

        public Long getId() { return id; }
        public String getOrderNumber() { return orderNumber; }
        public String getStatus() { return status; }
    }

    public static class SupplyChainReferenceDTO {
        private Long id;
        private String name;

        public SupplyChainReferenceDTO(Chains chain) {
            this.id = chain.getId();
            this.name = chain.getName();
        }

        public Long getId() { return id; }
        public String getName() { return name; }
    }

    public static class RecycledItemReferenceDTO {
        private Long id;
        private String name;
        private String status;

        public RecycledItemReferenceDTO(Items item) {
            this.id = item.getId();
            this.name = item.getName();
            this.status = item.getStatus();
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getStatus() { return status; }
    }

    // Getters omitted for brevity but should be included
    public Long getId() { return id; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public UserReferenceDTO getDistributor() { return distributor; }
    public UserReferenceDTO getSource() { return source; }
    public UserReferenceDTO getDestination() { return destination; }
    public MaterialRequestReferenceDTO getMaterialRequest() { return materialRequest; }
    public OrderReferenceDTO getOrder() { return order; }
    public SupplyChainReferenceDTO getSupplyChain() { return supplyChain; }
    public RecycledItemReferenceDTO getRecycledItem() { return recycledItem; }
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