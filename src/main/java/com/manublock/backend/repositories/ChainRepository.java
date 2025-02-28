package com.manublock.backend.repositories;

import com.manublock.backend.models.Chains;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChainRepository extends JpaRepository<Chains, Long> {
    List<Chains> findByCreatedBy_Id(Long userId);
}
