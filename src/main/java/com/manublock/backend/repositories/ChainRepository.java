package com.manublock.backend.repositories;

import com.manublock.backend.models.Chains;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChainRepository extends JpaRepository<Chains, Long> {
    @Query("SELECT DISTINCT c FROM Chains c JOIN c.nodes n WHERE n.assignedUser.id = :userId")
    List<Chains> findChainsByAssignedUser(@Param("userId") Long userId);
}

