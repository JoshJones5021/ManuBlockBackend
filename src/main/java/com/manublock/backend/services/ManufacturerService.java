package com.manublock.backend.services;

import com.manublock.backend.dto.MaterialRequestItemCreateDTO;
import com.manublock.backend.models.*;
import com.manublock.backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ManufacturerService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialRequestRepository materialRequestRepository;

    @Autowired
    private MaterialRequestItemRepository materialRequestItemRepository;

    @Autowired
    private ProductionBatchRepository productionBatchRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChainRepository chainRepository;

    @Autowired
    private ExtendedBlockchainService blockchainService;

    /**
     * Create a new product
     */
    public Product createProduct(String name, String description, String specifications,
                                 String sku, BigDecimal price, Long manufacturerId,
                                 Long supplyChainId, List<Long> requiredMaterialIds) {

        Users manufacturer = userRepository.findById(manufacturerId)
                .orElseThrow(() -> new RuntimeException("Manufacturer not found"));

        if (!manufacturer.getRole().equals(Roles.MANUFACTURER)) {
            throw new RuntimeException("User is not a manufacturer");
        }

        Chains supplyChain = chainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply chain not found"));

        // Get required materials
        List<Material> requiredMaterials = new ArrayList<>();
        if (requiredMaterialIds != null && !requiredMaterialIds.isEmpty()) {
            requiredMaterials = materialRepository.findAllById(requiredMaterialIds);

            if (requiredMaterials.size() != requiredMaterialIds.size()) {
                throw new RuntimeException("Some required materials not found");
            }
        }

        // Create product in database
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setSpecifications(specifications);
        product.setSku(sku);
        product.setPrice(price);
        product.setAvailableQuantity(0L); // Initially 0
        product.setManufacturer(manufacturer);
        product.setActive(true);
        product.setRequiredMaterials(requiredMaterials);
        product.setCreatedAt(new Date());
        product.setUpdatedAt(new Date());

        return productRepository.save(product);
    }

    /**
     * Update product details
     */
    public Product updateProduct(Long productId, String name, String description,
                                 String specifications, String sku, BigDecimal price,
                                 List<Long> requiredMaterialIds) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Get required materials if provided
        if (requiredMaterialIds != null && !requiredMaterialIds.isEmpty()) {
            List<Material> requiredMaterials = materialRepository.findAllById(requiredMaterialIds);

            if (requiredMaterials.size() != requiredMaterialIds.size()) {
                throw new RuntimeException("Some required materials not found");
            }

            product.setRequiredMaterials(requiredMaterials);
        }

        // Update properties
        product.setName(name);
        product.setDescription(description);
        product.setSpecifications(specifications);
        product.setSku(sku);
        product.setPrice(price);
        product.setUpdatedAt(new Date());

        return productRepository.save(product);
    }

    /**
     * Deactivate a product (logical deletion)
     */
    public Product deactivateProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setActive(false);
        product.setUpdatedAt(new Date());

        return productRepository.save(product);
    }

    /**
     * Request materials from a supplier
     */
    public MaterialRequest requestMaterials(Long manufacturerId,
                                            Long supplierId,
                                            Long supplyChainId,
                                            Long orderId,
                                            List<MaterialRequestItemCreateDTO> items,
                                            Date requestedDeliveryDate,
                                            String notes) {
        try {
            System.out.println("Starting requestMaterials in service - manufacturerId: " + manufacturerId);

            Users manufacturer = userRepository.findById(manufacturerId)
                    .orElseThrow(() -> {
                        System.err.println("Manufacturer not found with ID: " + manufacturerId);
                        return new RuntimeException("Manufacturer not found with ID: " + manufacturerId);
                    });

            Users supplier = userRepository.findById(supplierId)
                    .orElseThrow(() -> {
                        System.err.println("Supplier not found with ID: " + supplierId);
                        return new RuntimeException("Supplier not found with ID: " + supplierId);
                    });

            Chains supplyChain = chainRepository.findById(supplyChainId)
                    .orElseThrow(() -> {
                        System.err.println("Supply chain not found with ID: " + supplyChainId);
                        return new RuntimeException("Supply chain not found with ID: " + supplyChainId);
                    });

            // Optionally link to an order
            Order relatedOrder = null;
            if (orderId != null) {
                try {
                    relatedOrder = orderRepository.findById(orderId)
                            .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
                    System.out.println("Related order found: " + relatedOrder.getId());
                } catch (Exception e) {
                    System.err.println("Error finding related order: " + e.getMessage());
                    // Continue without the order
                }
            }

            // Validate items
            System.out.println("Validating " + (items != null ? items.size() : "null") + " material items");
            if (items == null || items.isEmpty()) {
                System.err.println("No items provided in the request");
                throw new RuntimeException("No materials specified for the request");
            }

            for (MaterialRequestItemCreateDTO item : items) {
                try {
                    System.out.println("Validating material ID: " + item.getMaterialId() +
                            ", Quantity: " + item.getQuantity());

                    Material material = materialRepository.findById(item.getMaterialId())
                            .orElseThrow(() -> new RuntimeException("Material not found: " + item.getMaterialId()));

                    if (!material.isActive()) {
                        System.err.println("Material is not active: " + material.getName());
                        throw new RuntimeException("Material is not active: " + material.getName());
                    }

                    if (!material.getSupplier().getId().equals(supplierId)) {
                        System.err.println("Material is not provided by the selected supplier: " + material.getName());
                        throw new RuntimeException("Material is not provided by the selected supplier: " + material.getName());
                    }
                } catch (Exception e) {
                    System.err.println("Error validating material item: " + e.getMessage());
                    throw e;
                }
            }

            // Generate request number
            String requestNumber = "REQ-" + Calendar.getInstance().getTimeInMillis();
            System.out.println("Generated request number: " + requestNumber);

            // Create material request
            MaterialRequest request = new MaterialRequest();
            request.setRequestNumber(requestNumber);
            request.setManufacturer(manufacturer);
            request.setSupplier(supplier);
            request.setSupplyChain(supplyChain);
            request.setRelatedOrder(relatedOrder);
            request.setStatus("Requested"); // Using default status if not provided
            request.setRequestedDeliveryDate(requestedDeliveryDate);
            request.setNotes(notes);
            request.setCreatedAt(new Date());
            request.setUpdatedAt(new Date());

            System.out.println("Saving material request");
            MaterialRequest savedRequest = materialRequestRepository.save(request);
            System.out.println("Material request saved with ID: " + savedRequest.getId());

            // Create request items - Fixed the type mismatch here
            List<MaterialRequestItem> requestItems = new ArrayList<>();
            for (MaterialRequestItemCreateDTO item : items) {  // Changed from MaterialRequestItemDTO to MaterialRequestItemCreateDTO
                try {
                    Material material = materialRepository.findById(item.getMaterialId()).get();
                    System.out.println("Creating item for material: " + material.getName() +
                            ", quantity: " + item.getQuantity());

                    MaterialRequestItem requestItem = new MaterialRequestItem();
                    requestItem.setMaterialRequest(savedRequest);
                    requestItem.setMaterial(material);
                    requestItem.setRequestedQuantity(item.getQuantity());
                    requestItem.setStatus("Requested");

                    MaterialRequestItem savedItem = materialRequestItemRepository.save(requestItem);
                    System.out.println("Material request item saved with ID: " + savedItem.getId());
                    requestItems.add(savedItem);
                } catch (Exception e) {
                    System.err.println("Error saving material request item: " + e.getMessage());
                    throw e;
                }
            }
            // Update request with items
            savedRequest.setItems(requestItems);
            // Now record on blockchain if blockchain service is available
            if (blockchainService != null) {
                try {
                    System.out.println("Recording material request on blockchain...");

                    Long blockchainItemId = generateUniqueBlockchainId();

                    Long totalQuantity = items.stream()
                            .mapToLong(MaterialRequestItemCreateDTO::getQuantity)
                            .sum();

                    // The actual method call depends on your BlockchainService implementation
                    // This is a simplified example:
                    CompletableFuture<String> future = blockchainService.createItem(
                            blockchainItemId,       // A unique ID for this material request
                            supplyChainId,          // The supply chain ID
                            totalQuantity,          // Use total quantity from all items (must be positive)
                            "material-request"      // Type of the item
                    );

                    // Handle the future completion asynchronously to avoid blocking
                    future.thenAccept(txHash -> {
                        try {
                            // Update the request with blockchain transaction hash
                            savedRequest.setBlockchainTxHash(txHash);
                            materialRequestRepository.save(savedRequest);
                            System.out.println("Material request recorded on blockchain: " + txHash);
                        } catch (Exception e) {
                            System.err.println("Error updating blockchain status: " + e.getMessage());
                        }
                    }).exceptionally(ex -> {
                        System.err.println("Blockchain transaction failed: " + ex.getMessage());
                        return null;
                    });
                } catch (Exception e) {
                    // Log error but don't prevent request creation if blockchain fails
                    System.err.println("Error with blockchain integration: " + e.getMessage());
                }
            }

            System.out.println("Material request process completed successfully");
            return savedRequest;
        } catch (Exception e) {
            System.err.println("Error in requestMaterials service method: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Create a production batch to manufacture products
     */
    public ProductionBatch createProductionBatch(Long manufacturerId, Long productId,
                                                 Long supplyChainId, Long orderId,
                                                 Long quantity, List<MaterialBatchItem> materials) {

        Users manufacturer = userRepository.findById(manufacturerId)
                .orElseThrow(() -> new RuntimeException("Manufacturer not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Chains supplyChain = chainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply chain not found"));

        // Optionally link to an order
        Order relatedOrder;
        if (orderId != null) {
            relatedOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
        } else {
            relatedOrder = null;
        }

        // Validate material items
        for (MaterialBatchItem item : materials) {
            Material material = materialRepository.findById(item.getMaterialId())
                    .orElseThrow(() -> new RuntimeException("Material not found: " + item.getMaterialId()));

            // Check if material has blockchain ID (has been allocated)
            if (item.getBlockchainItemId() == null) {
                throw new RuntimeException("Material has not been allocated on blockchain: " + material.getName());
            }
        }

        // Generate batch number
        String batchNumber = "BATCH-" + Calendar.getInstance().getTimeInMillis();

        // Create production batch
        ProductionBatch batch = new ProductionBatch();
        batch.setBatchNumber(batchNumber);
        batch.setManufacturer(manufacturer);
        batch.setProduct(product);
        batch.setSupplyChain(supplyChain);
        batch.setRelatedOrder(relatedOrder);
        batch.setQuantity(quantity);
        batch.setStatus("Planned");
        batch.setStartDate(new Date());
        batch.setCreatedAt(new Date());
        batch.setUpdatedAt(new Date());

        // Save all used materials
        List<Material> usedMaterials = new ArrayList<>();
        for (MaterialBatchItem item : materials) {
            Material material = materialRepository.findById(item.getMaterialId()).get();
            usedMaterials.add(material);
        }
        batch.setUsedMaterials(usedMaterials);

        ProductionBatch savedBatch = productionBatchRepository.save(batch);

        // Generate blockchain ID for the produced batch
        Long blockchainItemId = generateUniqueBlockchainId();

        // Create production on blockchain
        List<Long> sourceItemIds = materials.stream()
                .map(MaterialBatchItem::getBlockchainItemId)
                .collect(Collectors.toList());

        List<Long> inputQuantities = materials.stream()
                .map(MaterialBatchItem::getQuantity)
                .collect(Collectors.toList());

        // Use blockchain processing to create the product with all materials as parents
        blockchainService.processItem(
                        sourceItemIds,
                        blockchainItemId,
                        inputQuantities,
                        quantity,
                        "manufactured-product"
                )
                .thenAccept(txHash -> {
                    // Update batch with blockchain info
                    savedBatch.setBlockchainItemId(blockchainItemId);
                    savedBatch.setBlockchainTxHash(txHash);
                    savedBatch.setStatus("In Production");
                    productionBatchRepository.save(savedBatch);

                    // Update product available quantity
                    product.setAvailableQuantity(product.getAvailableQuantity() + quantity);
                    productRepository.save(product);

                    // If this batch is for an order, update the order item status
                    if (relatedOrder != null) {
                        for (OrderItem orderItem : relatedOrder.getItems()) {
                            if (orderItem.getProduct().getId().equals(productId)) {
                                orderItem.setStatus("In Production");
                                orderItemRepository.save(orderItem);
                            }
                        }

                        // Update order status
                        relatedOrder.setStatus("In Production");
                        orderRepository.save(relatedOrder);
                    }
                })
                .exceptionally(ex -> {
                    // Handle blockchain failure
                    savedBatch.setStatus("Failed");
                    productionBatchRepository.save(savedBatch);
                    throw new RuntimeException("Failed to create product on blockchain: " + ex.getMessage());
                });

        return savedBatch;
    }

    /**
     * Complete a production batch (quality control passed)
     */
    public ProductionBatch completeProductionBatch(Long batchId, String quality) {
        ProductionBatch batch = productionBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Production batch not found"));

        if (!batch.getStatus().equals("In Production")) {
            throw new RuntimeException("Batch is not in production");
        }

        // Update status
        batch.setStatus("Completed");
        batch.setQuality(quality);
        batch.setCompletionDate(new Date());
        batch.setUpdatedAt(new Date());

        ProductionBatch savedBatch = productionBatchRepository.save(batch);

        // Update the blockchain item status
        if (savedBatch.getBlockchainItemId() != null) {
            blockchainService.updateItemStatus(savedBatch.getBlockchainItemId(), 3) // 3 = COMPLETED
                    .thenAccept(txHash -> {
                        // Update transaction hash
                        savedBatch.setBlockchainTxHash(txHash);
                        productionBatchRepository.save(savedBatch);

                        // If this batch is for an order, update the order item status
                        if (savedBatch.getRelatedOrder() != null) {
                            Order order = savedBatch.getRelatedOrder();
                            for (OrderItem orderItem : order.getItems()) {
                                if (orderItem.getProduct().getId().equals(savedBatch.getProduct().getId())) {
                                    orderItem.setStatus("Ready for Shipment");
                                    orderItemRepository.save(orderItem);
                                }
                            }

                            // Check if all items are ready for shipment
                            boolean allReady = true;
                            for (OrderItem orderItem : order.getItems()) {
                                if (!orderItem.getStatus().equals("Ready for Shipment")) {
                                    allReady = false;
                                    break;
                                }
                            }

                            if (allReady) {
                                order.setStatus("Ready for Shipment");
                                orderRepository.save(order);
                            }
                        }
                    });
        }

        return savedBatch;
    }

    /**
     * Reject a production batch (quality control failed)
     */
    public ProductionBatch rejectProductionBatch(Long batchId, String reason) {
        ProductionBatch batch = productionBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Production batch not found"));

        if (!batch.getStatus().equals("In Production")) {
            throw new RuntimeException("Batch is not in production");
        }

        // Update status
        batch.setStatus("Rejected");
        batch.setQuality("Failed: " + reason);
        batch.setCompletionDate(new Date());
        batch.setUpdatedAt(new Date());

        // Reduce product available quantity
        Product product = batch.getProduct();
        product.setAvailableQuantity(product.getAvailableQuantity() - batch.getQuantity());
        productRepository.save(product);

        ProductionBatch savedBatch = productionBatchRepository.save(batch);

        // Update the blockchain item status
        if (savedBatch.getBlockchainItemId() != null) {
            blockchainService.updateItemStatus(savedBatch.getBlockchainItemId(), 4) // 4 = REJECTED
                    .thenAccept(txHash -> {
                        // Update transaction hash
                        savedBatch.setBlockchainTxHash(txHash);
                        productionBatchRepository.save(savedBatch);
                    });
        }

        return savedBatch;
    }

    /**
     * Generate a unique ID for blockchain items
     */
    private Long generateUniqueBlockchainId() {
        // Simple implementation - in production you might want something more sophisticated
        Random random = new Random();
        long id = random.nextLong(1_000_000_000, 9_999_999_999L);
        return id;
    }

    // GET methods

    public List<Product> getProductsByManufacturer(Long manufacturerId) {
        return productRepository.findByManufacturer_Id(manufacturerId);
    }

    public List<Product> getActiveProductsByManufacturer(Long manufacturerId) {
        return productRepository.findByActiveTrueAndManufacturer_Id(manufacturerId);
    }

    public List<MaterialRequest> getRequestsByManufacturer(Long manufacturerId) {
        return materialRequestRepository.findByManufacturer_Id(manufacturerId);
    }

    public List<MaterialRequest> getRequestsByManufacturerAndStatus(Long manufacturerId, String status) {
        return materialRequestRepository.findByManufacturer_IdAndStatus(manufacturerId, status);
    }

    public List<ProductionBatch> getBatchesByManufacturer(Long manufacturerId) {
        return productionBatchRepository.findByManufacturer_Id(manufacturerId);
    }

    public List<ProductionBatch> getBatchesByManufacturerAndStatus(Long manufacturerId, String status) {
        return productionBatchRepository.findByManufacturer_IdAndStatus(manufacturerId, status);
    }

    public List<Order> getOrdersByManufacturer(Long manufacturerId) {
        // Get products by manufacturer
        List<Product> products = productRepository.findByManufacturer_Id(manufacturerId);
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());

        // Find all orders containing these products
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Get all order items for these products
        List<OrderItem> orderItems = orderItemRepository.findByProduct_IdIn(productIds);
        Set<Long> orderIds = orderItems.stream().map(item -> item.getOrder().getId()).collect(Collectors.toSet());

        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }

        return orderRepository.findAllById(orderIds);
    }

    // Helper classes

    public static class MaterialRequestItemDTO {
        private Long materialId;
        private Long quantity;

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public Long getQuantity() {
            return quantity;
        }

        public void setQuantity(Long quantity) {
            this.quantity = quantity;
        }
    }

    public static class MaterialBatchItem {
        private Long materialId;
        private Long blockchainItemId; // ID of allocated material on blockchain
        private Long quantity;

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public Long getBlockchainItemId() {
            return blockchainItemId;
        }

        public void setBlockchainItemId(Long blockchainItemId) {
            this.blockchainItemId = blockchainItemId;
        }

        public Long getQuantity() {
            return quantity;
        }

        public void setQuantity(Long quantity) {
            this.quantity = quantity;
        }
    }
}