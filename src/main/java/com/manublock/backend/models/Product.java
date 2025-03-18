package com.manublock.backend.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column
    private String specifications; // JSON or text field with product specs

    @Column
    private String sku; // Stock keeping unit

    @Column
    private BigDecimal price;

    @Column
    private Long availableQuantity;

    @ManyToOne
    @JoinColumn(name = "manufacturer_id", nullable = false)
    private Users manufacturer;

    @Column
    private String productionBatch;

    @Column(nullable = false)
    private boolean active = true;

    @Column
    private Long blockchainItemId; // Reference to the blockchain item ID

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    // Replace the old ManyToMany relationship with OneToMany to the join table
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductMaterialQuantity> materialQuantities = new ArrayList<>();

    // Helper method to get materials with quantities for API responses
    public List<MaterialWithQuantity> getMaterialsWithQuantities() {
        List<MaterialWithQuantity> result = new ArrayList<>();
        if (materialQuantities != null) {
            for (ProductMaterialQuantity pmq : materialQuantities) {
                result.add(new MaterialWithQuantity(pmq.getMaterial(), pmq.getQuantity()));
            }
        }
        return result;
    }

    // Inner class for returning materials with quantities
    public static class MaterialWithQuantity {
        private Material material;
        private Long quantity;

        public MaterialWithQuantity(Material material, Long quantity) {
            this.material = material;
            this.quantity = quantity;
        }

        public Material getMaterial() {
            return material;
        }

        public Long getQuantity() {
            return quantity;
        }
    }

    // For backward compatibility with existing code
    public List<Material> getRequiredMaterials() {
        List<Material> materials = new ArrayList<>();
        if (materialQuantities != null) {
            for (ProductMaterialQuantity pmq : materialQuantities) {
                materials.add(pmq.getMaterial());
            }
        }
        return materials;
    }

    // Add helper method to set required materials with quantities
    public void addMaterialWithQuantity(Material material, Long quantity) {
        ProductMaterialQuantity pmq = new ProductMaterialQuantity();
        pmq.setProduct(this);
        pmq.setMaterial(material);
        pmq.setQuantity(quantity);
        materialQuantities.add(pmq);
    }

    // Clear all material quantities
    public void clearMaterialQuantities() {
        materialQuantities.clear();
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Long availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Users getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Users manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getProductionBatch() {
        return productionBatch;
    }

    public void setProductionBatch(String productionBatch) {
        this.productionBatch = productionBatch;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getBlockchainItemId() {
        return blockchainItemId;
    }

    public void setBlockchainItemId(Long blockchainItemId) {
        this.blockchainItemId = blockchainItemId;
    }

    public List<ProductMaterialQuantity> getMaterialQuantities() {
        return materialQuantities;
    }

    public void setMaterialQuantities(List<ProductMaterialQuantity> materialQuantities) {
        this.materialQuantities = materialQuantities;
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