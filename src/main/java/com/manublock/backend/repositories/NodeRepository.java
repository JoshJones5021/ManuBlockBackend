package com.manublock.backend.repositories;

import com.manublock.backend.models.SupplyChainNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeRepository extends JpaRepository<SupplyChainNode, Long> {
    List<SupplyChainNode> findBySupplyChainId(Long supplyChainId);
}
