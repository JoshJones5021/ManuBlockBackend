package com.manublock.backend.services;

import com.manublock.backend.models.*;
import com.manublock.backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
     * Create a new order with improved blockchain error handling
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

        // 🔗 Blockchain Transaction - Send total quantity with improved error handling
        try {
            // Calculate total quantity
            Long totalQuantity = items.stream()
                    .mapToLong(OrderItemDTO::getQuantity)
                    .sum();

            // Use a base orderId for blockchain (using timestamp to reduce chance of conflict)
            Long blockchainId = System.currentTimeMillis();

            CompletableFuture<String> blockchainTx = blockchainService.createItem(
                    blockchainId,          // Use the unique id as a base
                    supplyChain.getId(),   // Supply chain ID
                    totalQuantity,         // Total quantity
                    "ORDER",               // Type
                    customer.getId()       // Customer ID
            );

            // Set a timeout for the blockchain operation
            String blockchainTxHash = blockchainTx.get(60, TimeUnit.SECONDS); // 60 second timeout
            savedOrder.setBlockchainTxHash(blockchainTxHash);
            savedOrder = orderRepository.save(savedOrder);

            // Create blockchain items for each order item
            List<CompletableFuture<Long>> itemFutures = new ArrayList<>();

            for (OrderItem orderItem : orderItems) {
                try {
                    // Create an item on the blockchain for each order item with the improved method
                    CompletableFuture<Long> blockchainItemFuture = blockchainService.createItemOnBlockchain(
                            savedOrder.getId(),
                            supplyChain.getId(),
                            customer.getId(),
                            orderItem.getProduct().getId(),
                            orderItem.getQuantity()
                    );

                    itemFutures.add(blockchainItemFuture);
                } catch (Exception e) {
                    System.err.println("Error initiating blockchain item creation: " + e.getMessage());
                    // Continue with other items even if one fails
                }
            }

            // Wait for all futures to complete, with timeout
            try {
                CompletableFuture.allOf(itemFutures.toArray(new CompletableFuture[0]))
                        .get(120, TimeUnit.SECONDS); // 2 minute timeout for all items

                System.out.println("✅ All blockchain items created successfully for order: " + savedOrder.getId());
            } catch (Exception e) {
                System.err.println("⚠️ Some blockchain items may not have been created: " + e.getMessage());
                // Order is still created, just with potentially missing blockchain items
            }

        } catch (Exception e) {
            System.err.println("Blockchain order creation failed: " + e.getMessage());

            // Mark the order with a special status to indicate blockchain problem
            savedOrder.setStatus("Requested (Pending Blockchain)");
            savedOrder = orderRepository.save(savedOrder);

            // We still return the order, but with an indication that there was a blockchain issue
            System.err.println("Order created in database but blockchain registration failed: " + savedOrder.getId());
        }

        // Return the saved order
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

    /**
     * Generates a unique ID for blockchain items with optional namespace prefixing
     * to separate different types of items (products, orders, materials, etc.)
     *
     * @param baseValue An optional base value to include in the ID (like order ID)
     * @param namespace A numeric namespace (0-99) to prefix the ID with
     * @return A unique long value suitable for blockchain item identification
     */
    public static Long generateUniqueBlockchainId(Long baseValue, int namespace) {
        // Current time in milliseconds - provides basic uniqueness
        long timestamp = System.currentTimeMillis();

        // Random component to avoid collisions if multiple items created in same millisecond
        int random = new Random().nextInt(1000);

        // Ensure namespace is between 0-99
        namespace = Math.min(Math.max(namespace, 0), 99);

        // Format: NNBBBBBBTTTTTTRRR
        // NN: 2-digit namespace
        // BBBBBB: 6 most significant digits from base value (0 if null)
        // TTTTTT: 6 least significant digits from timestamp
        // RRR: 3-digit random value

        // Calculate components
        long baseComponent = (baseValue != null) ? (baseValue % 1_000_000) : 0;
        long timeComponent = timestamp % 1_000_000;

        // Combine all parts
        return (long) namespace * 1_000_000_000_000L + // Namespace prefix (NN0000000000000)
                baseComponent * 1_000_000L +            // Base value    (00BBBBBB000000)
                timeComponent * 1_000L +                // Timestamp     (0000000TTTTTT000)
                random;                                 // Random suffix (0000000000000RRR)
    }

    /**
     * Simplified version that just uses current time and a random factor
     */
    public static Long generateUniqueBlockchainId() {
        return generateUniqueBlockchainId(null, 0);
    }

    /**
     * Specialized version for order items that factors in order ID and product ID
     */
    public static Long generateOrderItemBlockchainId(Long orderId, Long productId, int attempt) {
        // Format the order and product IDs into a single base value
        // with retry attempt as a factor to avoid conflicts
        Long baseValue = orderId * 10_000L + productId + (attempt * 1_000_000L);

        // Use namespace 1 for order items
        return generateUniqueBlockchainId(baseValue, 1);
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