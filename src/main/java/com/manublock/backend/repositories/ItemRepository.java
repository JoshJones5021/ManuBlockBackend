package com.manublock.backend.repositories;

import com.manublock.backend.models.Items;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Items, Long> {
    List<Items> findBySupplyChain_Id(Long supplyChainId);
    List<Items> findByOwner_Id(Long ownerId);
    List<Items> findByStatus(String status);
    List<Items> findByItemType(String itemType);
    List<Items> findByOwner_IdAndItemType(Long ownerId, String itemType);
}