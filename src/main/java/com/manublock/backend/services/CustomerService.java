package com.manublock.backend.services;

import com.manublock.backend.models.*;
import com.manublock.backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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

        // 🔗 Blockchain Transaction - Send total quantity
        try {
            // Calculate total quantity
            Long totalQuantity = items.stream()
                    .mapToLong(OrderItemDTO::getQuantity)
                    .sum();

            CompletableFuture<String> blockchainTx = blockchainService.createOrderOnBlockchain(
                    savedOrder.getId(),        // Order ID
                    supplyChain.getId(),       // Supply chain ID
                    customer.getId(),          // Customer ID
                    totalQuantity              // Total quantity
            );

            // Optionally block or use async handling
            String blockchainTxHash = blockchainTx.get(); // Blocking for now, or handle asynchronously
            savedOrder.setBlockchainTxHash(blockchainTxHash);

            // Create blockchain items for each order item
            for (OrderItem orderItem : orderItems) {
                try {
                    // Create an item on the blockchain for each order item
                    CompletableFuture<Long> blockchainItemFuture = blockchainService.createItemOnBlockchain(
                            savedOrder.getId(),
                            supplyChain.getId(),
                            customer.getId(),
                            orderItem.getProduct().getId(),
                            orderItem.getQuantity()
                    );

                    // Get the blockchain item ID and set it on the order item
                    Long blockchainItemId = blockchainItemFuture.get();
                    orderItem.setBlockchainItemId(blockchainItemId);
                    orderItemRepository.save(orderItem);

                    // Create an entry in the Items table to track this blockchain item
                    Items blockchainItem = new Items();
                    blockchainItem.setId(blockchainItemId);
                    blockchainItem.setName("Order " + savedOrder.getOrderNumber() + " - " + orderItem.getProduct().getName());
                    blockchainItem.setItemType("product");
                    blockchainItem.setQuantity(orderItem.getQuantity());
                    blockchainItem.setOwner(customer);  // Set owner to customer
                    blockchainItem.setSupplyChain(supplyChain);  // Set supply chain
                    blockchainItem.setStatus("REQUESTED");
                    blockchainItem.setParentItemIds(new ArrayList<>()); // No parents for new items
                    blockchainItem.setBlockchainTxHash(blockchainTxHash);
                    blockchainItem.setBlockchainStatus("CREATED");
                    blockchainItem.setCreatedAt(new Date());
                    blockchainItem.setUpdatedAt(new Date());
                    itemRepository.save(blockchainItem);
                } catch (Exception e) {
                    System.err.println("Failed to create blockchain item for order item: " + e.getMessage());
                    // Decide whether to fail the entire transaction or continue with partial success
                }
            }

        } catch (Exception e) {
            System.err.println("Blockchain order creation failed: " + e.getMessage());
            // Optional: handle retry or mark order as 'Pending Blockchain'
        }

        // Final save with blockchain hash
        return orderRepository.save(savedOrder);
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

            // If item has a blockchain ID, update its status on the blockchain
            if (item.getBlockchainItemId() != null) {
                try {
                    // 4 typically represents REJECTED/CANCELLED status in your blockchain
                    blockchainService.updateItemStatus(
                            item.getBlockchainItemId(),
                            4,  // Status code for CANCELLED/REJECTED
                            order.getCustomer().getId()
                    ).thenAccept(txHash -> {
                        // Log the successful transaction
                        System.out.println("Order item cancelled on blockchain with hash: " + txHash);
                    }).exceptionally(ex -> {
                        // Log the error
                        System.err.println("Failed to cancel order item on blockchain: " + ex.getMessage());
                        return null;
                    });
                } catch (Exception e) {
                    System.err.println("Error updating blockchain status for cancelled item: " + e.getMessage());
                }
            }
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


    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomer_Id(customerId);
    }

    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public List<Product> getAvailableProducts() {
        return productRepository.findByActiveTrue();
    }


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