package com.manublock.backend.repositories;

import com.manublock.backend.models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_Id(Long orderId);

    List<OrderItem> findByProduct_Id(Long productId);

    List<OrderItem> findByStatus(String status);

    List<OrderItem> findByProduct_IdIn(List<Long> productIds);

    // 🔥 Support blockchain item lookup
    Optional<OrderItem> findByBlockchainItemId(Long blockchainItemId);

    // 🆕 Added method to find an order item by order ID and product ID
    Optional<OrderItem> findByOrder_IdAndProduct_Id(Long orderId, Long productId);
}