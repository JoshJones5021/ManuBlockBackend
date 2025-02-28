package com.manublock.backend.repositories;

import com.manublock.backend.models.Edges;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EdgeRepository extends JpaRepository<Edges, Long> {
    List<Edges> findBySource_IdOrTarget_Id(Long sourceId, Long targetId);
    List<Edges> findBySupplyChain_Id(Long supplyChainId);
}
