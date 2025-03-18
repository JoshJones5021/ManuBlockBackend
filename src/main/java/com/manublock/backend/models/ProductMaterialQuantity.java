package com.manublock.backend.models;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "product_material_quantities")
public class ProductMaterialQuantity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false)
    private Long quantity;

    // Default constructor
    public ProductMaterialQuantity() {
    }

    // Parameterized constructor
    public ProductMaterialQuantity(Product product, Material material, Long quantity) {
        this.product = product;
        this.material = material;
        this.quantity = quantity;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    // Override equals and hashCode for proper collection handling
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProductMaterialQuantity that = (ProductMaterialQuantity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (product != null ? !product.getId().equals(that.product.getId()) : that.product != null) return false;
        return material != null ? material.getId().equals(that.material.getId()) : that.material == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (product != null ? product.getId().hashCode() : 0);
        result = 31 * result + (material != null ? material.getId().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ProductMaterialQuantity{" +
                "id=" + id +
                ", product=" + (product != null ? product.getId() : null) +
                ", material=" + (material != null ? material.getId() : null) +
                ", quantity=" + quantity +
                '}';
    }
}