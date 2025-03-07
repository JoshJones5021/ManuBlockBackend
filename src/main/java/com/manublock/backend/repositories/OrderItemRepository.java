package com.manublock.backend.repositories;

import com.manublock.backend.models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder_Id(Long orderId);
    List<OrderItem> findByProduct_Id(Long productId);
    List<OrderItem> findByStatus(String status);

    // Additional methods
    List<OrderItem> findByProduct_IdIn(List<Long> productIds);
}