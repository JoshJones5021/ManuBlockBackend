package com.manublock.backend.controllers;

import com.manublock.backend.dto.OrderResponseDTO;
import com.manublock.backend.dto.ProductDTO;
import com.manublock.backend.models.Order;
import com.manublock.backend.models.Product;
import com.manublock.backend.services.CustomerService;
import com.manublock.backend.utils.DTOConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> payload, @RequestHeader(value = "X-Customer-Id", required = false) Long headerCustomerId) {
        try {
            // Try to get customerId from the header first
            Long customerId = headerCustomerId;

            // If not in header, try to get from payload
            if (customerId == null && payload.containsKey("customerId")) {
                customerId = Long.valueOf(payload.get("customerId").toString());
            }

            // If still null, throw an exception
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Customer ID is required. Provide it in the request body or X-Customer-Id header.");
            }

            Long supplyChainId = Long.valueOf(payload.get("supplyChainId").toString());

            // Get items from the payload
            List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) payload.get("items");

            // Convert generic maps to OrderItemDTO objects
            List<CustomerService.OrderItemDTO> items = itemMaps.stream()
                    .map(map -> {
                        CustomerService.OrderItemDTO item = new CustomerService.OrderItemDTO();
                        item.setProductId(Long.valueOf(map.get("productId").toString()));
                        item.setQuantity(Long.valueOf(map.get("quantity").toString()));
                        return item;
                    })
                    .collect(Collectors.toList());

            String shippingAddress = (String) payload.get("shippingAddress");

            Date requestedDeliveryDate = payload.get("requestedDeliveryDate") != null ?
                    new Date(Long.parseLong(payload.get("requestedDeliveryDate").toString())) : null;

            String deliveryNotes = (String) payload.get("deliveryNotes");

            Order order = customerService.createOrder(
                    customerId, supplyChainId, items, shippingAddress,
                    requestedDeliveryDate, deliveryNotes);

            // Convert to DTO before returning
            OrderResponseDTO orderDTO = DTOConverter.convertToOrderDTO(order);
            return ResponseEntity.ok(orderDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating order: " + e.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            Order order = customerService.cancelOrder(orderId);
            // Convert to DTO before returning
            OrderResponseDTO orderDTO = DTOConverter.convertToOrderDTO(order);
            return ResponseEntity.ok(orderDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error cancelling order: " + e.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/confirm")
    public ResponseEntity<?> confirmDelivery(@PathVariable Long orderId) {
        try {
            Order order = customerService.confirmDelivery(orderId);
            // Convert to DTO before returning
            OrderResponseDTO orderDTO = DTOConverter.convertToOrderDTO(order);
            return ResponseEntity.ok(orderDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error confirming delivery: " + e.getMessage());
        }
    }

    @GetMapping("/orders/{customerId}")
    public ResponseEntity<?> getOrdersByCustomer(@PathVariable Long customerId) {
        try {
            System.out.println("Controller: Getting orders for customer ID: " + customerId);
            List<Order> orders = customerService.getOrdersByCustomer(customerId);
            System.out.println("Controller: Found " + orders.size() + " orders");

            // Convert to DTOs using the enhanced converter
            List<OrderResponseDTO> orderDTOs = DTOConverter.convertToOrderDTOList(orders);

            return ResponseEntity.ok(orderDTOs);
        } catch (Exception e) {
            System.err.println("Controller Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving orders: " + e.getMessage());
        }
    }

    @GetMapping("/orders/number/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            Order order = customerService.getOrderByNumber(orderNumber);
            // Convert to DTO before returning
            OrderResponseDTO orderDTO = DTOConverter.convertToOrderDTO(order);
            return ResponseEntity.ok(orderDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving order: " + e.getMessage());
        }
    }

    @GetMapping("/products/available")
    public ResponseEntity<?> getAvailableProducts() {
        try {
            List<Product> products = customerService.getAvailableProducts();

            // Convert the Product entities to ProductDTO objects to avoid recursion
            List<ProductDTO> productDTOs = products.stream()
                    .map(ProductDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(productDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving available products: " + e.getMessage());
        }
    }
}