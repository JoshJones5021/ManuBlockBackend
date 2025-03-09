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

    // ✅ Fetch all nodes assigned to a user (to unassign before user deletion)
    List<Nodes> findByAssignedUser_Id(Long userId);
}
