package com.manublock.backend.repositories;

import com.manublock.backend.models.Edge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EdgeRepository extends JpaRepository<Edge, Long> {
    List<Edge> findBySupplyChainId(Long supplyChainId);
}
