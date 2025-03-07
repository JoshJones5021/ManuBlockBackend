package com.manublock.backend.repositories;

import com.manublock.backend.models.Transport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransportRepository extends JpaRepository<Transport, Long> {
    List<Transport> findByDistributor_Id(Long distributorId);
    List<Transport> findBySource_Id(Long sourceId);
    List<Transport> findByDestination_Id(Long destinationId);
    List<Transport> findBySupplyChain_Id(Long supplyChainId);
    List<Transport> findByStatus(String status);
    List<Transport> findByType(String type);
    List<Transport> findByMaterialRequest_Id(Long materialRequestId);
    List<Transport> findByOrder_Id(Long orderId);
    Transport findByTrackingNumber(String trackingNumber);

    // Additional methods
    List<Transport> findByDistributor_IdAndStatus(Long distributorId, String status);
    List<Transport> findByDistributor_IdAndType(Long distributorId, String type);
}