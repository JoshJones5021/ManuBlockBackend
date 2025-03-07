package com.manublock.backend.repositories;

import com.manublock.backend.models.ProductionBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, Long> {
    List<ProductionBatch> findByManufacturer_Id(Long manufacturerId);
    List<ProductionBatch> findByProduct_Id(Long productId);
    List<ProductionBatch> findBySupplyChain_Id(Long supplyChainId);
    List<ProductionBatch> findByStatus(String status);
    List<ProductionBatch> findByRelatedOrder_Id(Long orderId);
    ProductionBatch findByBatchNumber(String batchNumber);

    // Additional methods
    List<ProductionBatch> findByManufacturer_IdAndStatus(Long manufacturerId, String status);
}