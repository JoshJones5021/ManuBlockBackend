package com.manublock.backend.repositories;

import com.manublock.backend.models.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findBySupplier_Id(Long supplierId);
    List<Material> findByActiveTrue();
    List<Material> findByActiveTrueAndSupplier_Id(Long supplierId);
    Optional<Material> findByBlockchainItemId(Long blockchainItemId);
    List<Material> findByNameAndSupplier_Id(String name, Long supplierId);
}