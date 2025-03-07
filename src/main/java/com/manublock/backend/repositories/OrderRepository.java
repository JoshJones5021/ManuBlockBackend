package com.manublock.backend.repositories;

import com.manublock.backend.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer_Id(Long customerId);
    List<Order> findBySupplyChain_Id(Long supplyChainId);
    List<Order> findByStatus(String status);
    Order findByOrderNumber(String orderNumber);

    // Additional methods
    List<Order> findByCustomer_IdAndStatus(Long customerId, String status);
}