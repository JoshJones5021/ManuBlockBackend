package com.manublock.backend.repositories;

import com.manublock.backend.models.MaterialRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRequestRepository extends JpaRepository<MaterialRequest, Long> {
    List<MaterialRequest> findByManufacturer_Id(Long manufacturerId);
    List<MaterialRequest> findBySupplier_Id(Long supplierId);
    List<MaterialRequest> findBySupplyChain_Id(Long supplyChainId);
    List<MaterialRequest> findByStatus(String status);
    MaterialRequest findByRequestNumber(String requestNumber);

    // Additional methods
    List<MaterialRequest> findBySupplier_IdAndStatus(Long supplierId, String status);
    List<MaterialRequest> findByManufacturer_IdAndStatus(Long manufacturerId, String status);
}