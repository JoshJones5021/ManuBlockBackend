package com.manublock.backend.repositories;

import com.manublock.backend.models.ProductMaterialQuantity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductMaterialQuantityRepository extends JpaRepository<ProductMaterialQuantity, Long> {
    List<ProductMaterialQuantity> findByProductId(Long productId);
}