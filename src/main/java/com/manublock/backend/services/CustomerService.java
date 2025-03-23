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

        // Start blockchain operations asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                processBlockchainOperations(savedOrder, customer, supplyChain, orderItems);
            } catch (Exception e) {
                System.err.println("Blockchain operations failed: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Return the saved order immediately without waiting for blockchain
        return savedOrder;
    }

    /**
     * Process blockchain operations separately from the main request thread
     */
    private void processBlockchainOperations(Order order, Users customer, Chains supplyChain, List<OrderItem> orderItems) {
        try {
            // Calculate total quantity
            Long totalQuantity = orderItems.stream()
                    .mapToLong(OrderItem::getQuantity)
                    .sum();

            // Generate a blockchain ID that's definitely unique
            Long blockchainId = generateUniqueBlockchainId();

            // Use the blockchain ID from the supply chain if available
            Long chainId = supplyChain.getBlockchainId() != null ?
                    supplyChain.getBlockchainId() : supplyChain.getId();

            // Create order on blockchain
            String txHash = blockchainService.createItem(
                    blockchainId,
                    chainId,
                    totalQuantity,
                    "ORDER",
                    customer.getId()
            ).get(60, TimeUnit.SECONDS); // Wait for this transaction to complete

            // Update order with blockchain hash
            order.setBlockchainTxHash(txHash);
            orderRepository.save(order);
            System.out.println("✅ Order recorded on blockchain with hash: " + txHash);

            // Create product items separately
            for (OrderItem orderItem : orderItems) {
                try {
                    // Generate a unique ID for each product item
                    Long productItemId = generateUniqueBlockchainId();

                    // Create the blockchain item
                    blockchainService.createItem(
                            productItemId,
                            chainId,
                            orderItem.getQuantity(),
                            "product",
                            customer.getId()
                    ).thenAccept(productTxHash -> {
                        try {
                            // Update the order item with blockchain ID
                            orderItem.setBlockchainItemId(productItemId);
                            orderItemRepository.save(orderItem);
                            System.out.println("✅ Product item created for order item: " + orderItem.getId());
                        } catch (Exception e) {
                            System.err.println("Error updating order item: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Error creating blockchain item for product: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error in blockchain operations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates a unique blockchain ID to avoid conflicts
     */
    private Long generateUniqueBlockchainId() {
        long timestamp = System.currentTimeMillis();
        int randomComponent = new Random().nextInt(1000000);
        return timestamp * 1000L + randomComponent;
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