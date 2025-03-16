package com.manublock.backend.dto;

public class OrderItemDTO {
    private Long productId;
    private Long quantity;

    // Default constructor needed for deserialization
    public OrderItemDTO() {
    }

    // Getters and setters
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }
}