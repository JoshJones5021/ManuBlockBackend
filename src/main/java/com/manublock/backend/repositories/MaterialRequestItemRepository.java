package com.manublock.backend.repositories;

import com.manublock.backend.models.MaterialRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRequestItemRepository extends JpaRepository<MaterialRequestItem, Long> {
    List<MaterialRequestItem> findByMaterialRequest_Id(Long materialRequestId);
    List<MaterialRequestItem> findByMaterial_Id(Long materialId);
    List<MaterialRequestItem> findByStatus(String status);
}