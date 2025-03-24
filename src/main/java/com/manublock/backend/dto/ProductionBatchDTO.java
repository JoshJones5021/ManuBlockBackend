package com.manublock.backend.dto;

import com.manublock.backend.models.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ProductionBatchDTO {
    private Long id;
    private String batchNumber;
    private UserReferenceDTO manufacturer;
    private ProductReferenceDTO product;
    private SupplyChainReferenceDTO supplyChain;
    private OrderReferenceDTO relatedOrder;
    private Long quantity;
    private String status;
    private Date startDate;
    private Date completionDate;
    private String quality;
    private Long blockchainItemId;
    private String blockchainTxHash;
    private List<MaterialReferenceDTO> usedMaterials;
    private Date createdAt;
    private Date updatedAt;

    public ProductionBatchDTO(ProductionBatch batch) {
        this.id = batch.getId();
        this.batchNumber = batch.getBatchNumber();

        // Manufacturer reference
        if (batch.getManufacturer() != null) {
            this.manufacturer = new UserReferenceDTO(batch.getManufacturer());
        }

        // Product reference
        if (batch.getProduct() != null) {
            this.product = new ProductReferenceDTO(batch.getProduct());
        }

        // Supply chain reference
        if (batch.getSupplyChain() != null) {
            this.supplyChain = new SupplyChainReferenceDTO(batch.getSupplyChain());
        }

        // Related order reference
        if (batch.getRelatedOrder() != null) {
            this.relatedOrder = new OrderReferenceDTO(batch.getRelatedOrder());
        }

        this.quantity = batch.getQuantity();
        this.status = batch.getStatus();
        this.startDate = batch.getStartDate();
        this.completionDate = batch.getCompletionDate();
        this.quality = batch.getQuality();
        this.blockchainItemId = batch.getBlockchainItemId();
        this.blockchainTxHash = batch.getBlockchainTxHash();

        // Used materials references
        if (batch.getUsedMaterials() != null) {
            this.usedMaterials = batch.getUsedMaterials().stream()
                    .map(MaterialReferenceDTO::new)
                    .collect(Collectors.toList());
        }

        this.createdAt = batch.getCreatedAt();
        this.updatedAt = batch.getUpdatedAt();
    }

    // Simple reference classes to avoid recursion
    public static class UserReferenceDTO {
        private Long id;
        private String username;

        public UserReferenceDTO(Users user) {
            this.id = user.getId();
            this.username = user.getUsername();
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
    }

    public static class ProductReferenceDTO {
        private Long id;
        private String name;
        private String sku;

        public ProductReferenceDTO(Product product) {
            this.id = product.getId();
            this.name = product.getName();
            this.sku = product.getSku();
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getSku() { return sku; }
    }

    public static class SupplyChainReferenceDTO {
        private Long id;
        private String name;

        public SupplyChainReferenceDTO(Chains supplyChain) {
            this.id = supplyChain.getId();
            this.name = supplyChain.getName();
        }

        public Long getId() { return id; }
        public String getName() { return name; }
    }

    public static class OrderReferenceDTO {
        private Long id;
        private String orderNumber;

        public OrderReferenceDTO(Order order) {
            this.id = order.getId();
            this.orderNumber = order.getOrderNumber();
        }

        public Long getId() { return id; }
        public String getOrderNumber() { return orderNumber; }
    }

    public static class MaterialReferenceDTO {
        private Long id;
        private String name;
        private String unit;

        public MaterialReferenceDTO(Material material) {
            this.id = material.getId();
            this.name = material.getName();
            this.unit = material.getUnit();
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getUnit() { return unit; }
    }

    // Getters
    public Long getId() { return id; }
    public String getBatchNumber() { return batchNumber; }
    public UserReferenceDTO getManufacturer() { return manufacturer; }
    public ProductReferenceDTO getProduct() { return product; }
    public SupplyChainReferenceDTO getSupplyChain() { return supplyChain; }
    public OrderReferenceDTO getRelatedOrder() { return relatedOrder; }
    public Long getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public Date getStartDate() { return startDate; }
    public Date getCompletionDate() { return completionDate; }
    public String getQuality() { return quality; }
    public Long getBlockchainItemId() { return blockchainItemId; }
    public String getBlockchainTxHash() { return blockchainTxHash; }
    public List<MaterialReferenceDTO> getUsedMaterials() { return usedMaterials; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
}