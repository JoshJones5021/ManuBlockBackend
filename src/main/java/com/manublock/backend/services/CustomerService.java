package com.manublock.backend.services;

import com.manublock.backend.models.*;
import com.manublock.backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChainRepository chainRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ExtendedBlockchainService blockchainService;

    /**
     * Create a new order
     */
    public Order createOrder(Long customerId, Long supplyChainId,
                             List<OrderItemDTO> items,
                             String shippingAddress,
                             Date requestedDeliveryDate,
                             String deliveryNotes) {

        Users customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (!customer.getRole().equals(Roles.CUSTOMER)) {
            throw new RuntimeException("User is not a customer");
        }

        Chains supplyChain = chainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply chain not found"));

        // Validate order items
        for (OrderItemDTO item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));

            if (!product.isActive()) {
                throw new RuntimeException("Product is not active: " + product.getName());
            }
        }

        // Generate order number
        String orderNumber = "ORD-" + Calendar.getInstance().getTimeInMillis();

        // Create order
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setCustomer(customer);
        order.setSupplyChain(supplyChain);
        order.setStatus("Requested");
        order.setShippingAddress(shippingAddress);
        order.setRequestedDeliveryDate(requestedDeliveryDate);
        order.setDeliveryNotes(deliveryNotes);
        order.setCreatedAt(new Date());
        order.setUpdatedAt(new Date());

        Order savedOrder = orderRepository.save(order);

        // Create order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemDTO item : items) {
            Product product = productRepository.findById(item.getProductId()).get();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
            orderItem.setStatus("Requested");

            orderItems.add(orderItemRepository.save(orderItem));
        }

        // Update order with items
        savedOrder.setItems(orderItems);

        return savedOrder;
    }

    /**
     * Cancel an order (if possible)
     */
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Check if order can be cancelled (not already in production)
        if (order.getStatus().equals("In Production") ||
                order.getStatus().equals("Ready for Shipment") ||
                order.getStatus().equals("Delivered") ||
                order.getStatus().equals("Completed")) {
            throw new RuntimeException("Order cannot be cancelled in its current state: " + order.getStatus());
        }

        // Update order status
        order.setStatus("Cancelled");
        order.setUpdatedAt(new Date());

        // Update order items
        for (OrderItem item : order.getItems()) {
            item.setStatus("Cancelled");
            orderItemRepository.save(item);
        }

        return orderRepository.save(order);
    }

    /**
     * Confirm order delivery
     */
    /**
     * Confirm order delivery
     */
    public Order confirmDelivery(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getStatus().equals("Delivered")) {
            throw new RuntimeException("Order is not in 'Delivered' status");
        }

        // Update order status
        order.setStatus("Completed");
        order.setActualDeliveryDate(new Date());
        order.setUpdatedAt(new Date());

        // Update order items
        for (OrderItem item : order.getItems()) {
            item.setStatus("Completed");
            orderItemRepository.save(item);

            // Update blockchain status if item has a blockchain ID
            if (item.getBlockchainItemId() != null) {
                blockchainService.updateItemStatus(
                        item.getBlockchainItemId(),
                        3,  // 3 = COMPLETED status
                        order.getCustomer().getId()
                ).thenAccept(txHash -> {
                    // Log the successful transaction
                    System.out.println("Blockchain item status updated with hash: " + txHash);

                    // Update Items table record
                    try {
                        Optional<Items> itemOpt = itemRepository.findById(item.getBlockchainItemId());
                        if (itemOpt.isPresent()) {
                            Items blockchainItem = itemOpt.get();
                            blockchainItem.setStatus("COMPLETED");
                            blockchainItem.setUpdatedAt(new Date());
                            blockchainItem.setBlockchainTxHash(txHash);
                            itemRepository.save(blockchainItem);
                        }
                    } catch (Exception e) {
                        System.err.println("Error updating Items record: " + e.getMessage());
                    }
                }).exceptionally(ex -> {
                    // Log the error
                    System.err.println("Failed to update blockchain status: " + ex.getMessage());
                    return null;
                });
            }
        }

        return orderRepository.save(order);
    }

    // GET methods

    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomer_Id(customerId);
    }

    public List<Order> getOrdersByCustomerAndStatus(Long customerId, String status) {
        return orderRepository.findByCustomer_IdAndStatus(customerId, status);
    }

    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public List<Product> getAvailableProducts() {
        return productRepository.findByActiveTrue();
    }

    // Helper class

    public static class OrderItemDTO {
        private Long productId;
        private Long quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Long getQuantity() {
            return quantity;
        }

        public void setQuantity(Long quantity) {
            this.quantity = quantity;
        }
    }
}