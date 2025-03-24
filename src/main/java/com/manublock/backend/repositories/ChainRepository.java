package com.manublock.backend.repositories;

import com.manublock.backend.models.Chains;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChainRepository extends JpaRepository<Chains, Long> {
    @Query("SELECT DISTINCT c FROM Chains c JOIN c.nodes n WHERE n.assignedUser.id = :userId")
    List<Chains> findChainsByAssignedUser(@Param("userId") Long userId);

    /**
     * Find a supply chain by its blockchain ID
     *
     * @param blockchainId The blockchain ID to search for
     * @return The supply chain with the matching blockchain ID, if found
     */
    Optional<Chains> findByBlockchainId(Long blockchainId);
}