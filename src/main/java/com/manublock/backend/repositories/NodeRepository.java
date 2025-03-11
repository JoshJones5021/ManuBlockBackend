package com.manublock.backend.repositories;

import com.manublock.backend.models.Nodes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeRepository extends JpaRepository<Nodes, Long> {
    List<Nodes> findBySupplyChain_Id(Long supplyChainId);
    List<Nodes> findByStatus(String status);
    List<Nodes> findBySupplyChain_IdAndStatus(Long supplyChainId, String status);
    List<Nodes> findByAssignedUser_Id(Long userId);

    // New methods for supply chain finalization and user access
    List<Nodes> findBySupplyChain_IdAndAssignedUser_Id(Long supplyChainId, Long userId);

    // Find nodes by role and supply chain
    List<Nodes> findBySupplyChain_IdAndRole(Long supplyChainId, String role);
}