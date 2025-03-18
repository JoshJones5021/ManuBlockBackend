package com.manublock.backend.dto;

import com.manublock.backend.models.Order;
import com.manublock.backend.models.Product;
import com.manublock.backend.models.ProductionBatch;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ProductionBatchDTO {
    private Long id;
    private String batchNumber;
    private Long manufacturerId;
    private String manufacturerName;
    private ProductDTO product;
    private Long supplyChainId;
    private String supplyChainName;
    private Long quantity;
    private String status;
    private Date startDate;
    private Date completionDate;
    private String quality;
    private Long blockchainItemId;
    private String blockchainTxHash;
    private List<MaterialDTO> usedMaterials;
    private OrderSummaryDTO relatedOrder;
    private Date createdAt;
    private Date updatedAt;

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public OrderSummaryDTO getRelatedOrder() {
        return relatedOrder;
    }

    public void setRelatedOrder(OrderSummaryDTO relatedOrder) {
        this.relatedOrder = relatedOrder;
    }

    public List<MaterialDTO> getUsedMaterials() {
        return usedMaterials;
    }

    public void setUsedMaterials(List<MaterialDTO> usedMaterials) {
        this.usedMaterials = usedMaterials;
    }

    public String getBlockchainTxHash() {
        return blockchainTxHash;
    }

    public void setBlockchainTxHash(String blockchainTxHash) {
        this.blockchainTxHash = blockchainTxHash;
    }

    public Long getBlockchainItemId() {
        return blockchainItemId;
    }

    public void setBlockchainItemId(Long blockchainItemId) {
        this.blockchainItemId = blockchainItemId;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getSupplyChainName() {
        return supplyChainName;
    }

    public void setSupplyChainName(String supplyChainName) {
        this.supplyChainName = supplyChainName;
    }

    public Long getSupplyChainId() {
        return supplyChainId;
    }

    public void setSupplyChainId(Long supplyChainId) {
        this.supplyChainId = supplyChainId;
    }

    public ProductDTO getProduct() {
        return product;
    }

    public void setProduct(ProductDTO product) {
        this.product = product;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    public Long getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(Long manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public static class ProductDTO {
        private Long id;
        private String name;
        private String sku;

        public ProductDTO(Product product) {
            this.id = product.getId();
            this.name = product.getName();
            this.sku = product.getSku();
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public static class OrderSummaryDTO {
        private Long id;
        private String orderNumber;

        public OrderSummaryDTO(Order order) {
            this.id = order.getId();
            this.orderNumber = order.getOrderNumber();
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public ProductionBatchDTO(ProductionBatch batch) {
        this.id = batch.getId();
        this.batchNumber = batch.getBatchNumber();

        if (batch.getManufacturer() != null) {
            this.manufacturerId = batch.getManufacturer().getId();
            this.manufacturerName = batch.getManufacturer().getUsername();
        }

        if (batch.getProduct() != null) {
            this.product = new ProductDTO(batch.getProduct());
        }

        if (batch.getSupplyChain() != null) {
            this.supplyChainId = batch.getSupplyChain().getId();
            this.supplyChainName = batch.getSupplyChain().getName();
        }

        this.quantity = batch.getQuantity();
        this.status = batch.getStatus();
        this.startDate = batch.getStartDate();
        this.completionDate = batch.getCompletionDate();
        this.quality = batch.getQuality();
        this.blockchainItemId = batch.getBlockchainItemId();
        this.blockchainTxHash = batch.getBlockchainTxHash();

        if (batch.getUsedMaterials() != null) {
            this.usedMaterials = batch.getUsedMaterials().stream()
                    .map(material -> new MaterialDTO(material))
                    .collect(Collectors.toList());
        }

        if (batch.getRelatedOrder() != null) {
            this.relatedOrder = new OrderSummaryDTO(batch.getRelatedOrder());
        }

        this.createdAt = batch.getCreatedAt();
        this.updatedAt = batch.getUpdatedAt();
    }
}