package com.manublock.backend.controllers;

import com.manublock.backend.models.Order;
import com.manublock.backend.models.Product;
import com.manublock.backend.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> payload) {
        try {
            Long customerId = Long.valueOf(payload.get("customerId").toString());
            Long supplyChainId = Long.valueOf(payload.get("supplyChainId").toString());

            @SuppressWarnings("unchecked")
            List<CustomerService.OrderItemDTO> items =
                    (List<CustomerService.OrderItemDTO>) payload.get("items");

            String shippingAddress = (String) payload.get("shippingAddress");

            Date requestedDeliveryDate = payload.get("requestedDeliveryDate") != null ?
                    new Date(Long.parseLong(payload.get("requestedDeliveryDate").toString())) : null;

            String deliveryNotes = (String) payload.get("deliveryNotes");

            Order order = customerService.createOrder(
                    customerId, supplyChainId, items, shippingAddress,
                    requestedDeliveryDate, deliveryNotes);

            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating order: " + e.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            Order order = customerService.cancelOrder(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error cancelling order: " + e.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/confirm")
    public ResponseEntity<?> confirmDelivery(@PathVariable Long orderId) {
        try {
            Order order = customerService.confirmDelivery(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error confirming delivery: " + e.getMessage());
        }
    }

    @GetMapping("/orders/{customerId}")
    public ResponseEntity<?> getOrdersByCustomer(@PathVariable Long customerId) {
        try {
            List<Order> orders = customerService.getOrdersByCustomer(customerId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving orders: " + e.getMessage());
        }
    }

    @GetMapping("/orders/{customerId}/status/{status}")
    public ResponseEntity<?> getOrdersByStatus(
            @PathVariable Long customerId,
            @PathVariable String status) {
        try {
            List<Order> orders = customerService.getOrdersByCustomerAndStatus(customerId, status);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving orders by status: " + e.getMessage());
        }
    }

    @GetMapping("/orders/number/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            Order order = customerService.getOrderByNumber(orderNumber);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving order: " + e.getMessage());
        }
    }

    @GetMapping("/products/available")
    public ResponseEntity<?> getAvailableProducts() {
        try {
            List<Product> products = customerService.getAvailableProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving available products: " + e.getMessage());
        }
    }
}