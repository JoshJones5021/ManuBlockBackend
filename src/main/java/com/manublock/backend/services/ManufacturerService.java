package com.manublock.backend.services;

import com.manublock.backend.dto.MaterialDTO;
import com.manublock.backend.dto.MaterialQuantityDTO;
import com.manublock.backend.dto.MaterialRequestItemCreateDTO;
import com.manublock.backend.models.*;
import com.manublock.backend.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private ProductMaterialQuantityRepository productMaterialQuantityRepository;

    @Autowired
    private ItemRepository itemRepository;

    /**
     * Create a new product with material quantities
     */
    public Product createProduct(String name, String description, String specifications,
                                 String sku, BigDecimal price, Long manufacturerId,
                                 Long supplyChainId, List<MaterialQuantityDTO> materialQuantities) {

        Users manufacturer = userRepository.findById(manufacturerId)
                .orElseThrow(() -> new RuntimeException("Manufacturer not found"));

        if (!manufacturer.getRole().equals(Roles.MANUFACTURER)) {
            throw new RuntimeException("User is not a manufacturer");
        }

        Chains supplyChain = chainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply chain not found"));

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
        product.setCreatedAt(new Date());
        product.setUpdatedAt(new Date());

        // Save the product first to get an ID
        Product savedProduct = productRepository.save(product);

        // Now add material quantities
        if (materialQuantities != null && !materialQuantities.isEmpty()) {
            for (MaterialQuantityDTO mqDTO : materialQuantities) {
                Material material = materialRepository.findById(mqDTO.getMaterialId())
                        .orElseThrow(() -> new RuntimeException("Material not found: " + mqDTO.getMaterialId()));

                ProductMaterialQuantity pmq = new ProductMaterialQuantity();
                pmq.setProduct(savedProduct);
                pmq.setMaterial(material);
                pmq.setQuantity(mqDTO.getQuantity());
                productMaterialQuantityRepository.save(pmq);
            }
        }

        // Reload the product to include the newly saved relationships
        return productRepository.findById(savedProduct.getId()).orElse(savedProduct);
    }

    /**
     * Update product details
     */
    public Product updateProduct(Long productId, String name, String description,
                                 String specifications, String sku, BigDecimal price,
                                 List<MaterialQuantityDTO> materialQuantities) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Update properties
        product.setName(name);
        product.setDescription(description);
        product.setSpecifications(specifications);
        product.setSku(sku);
        product.setPrice(price);
        product.setUpdatedAt(new Date());

        // Save the product updates
        Product savedProduct = productRepository.save(product);

        // Delete existing material quantities
        List<ProductMaterialQuantity> existingQuantities =
                productMaterialQuantityRepository.findByProductId(productId);
        productMaterialQuantityRepository.deleteAll(existingQuantities);

        // Add new material quantities
        if (materialQuantities != null && !materialQuantities.isEmpty()) {
            for (MaterialQuantityDTO mqDTO : materialQuantities) {
                Material material = materialRepository.findById(mqDTO.getMaterialId())
                        .orElseThrow(() -> new RuntimeException("Material not found: " + mqDTO.getMaterialId()));

                ProductMaterialQuantity pmq = new ProductMaterialQuantity();
                pmq.setProduct(savedProduct);
                pmq.setMaterial(material);
                pmq.setQuantity(mqDTO.getQuantity());
                productMaterialQuantityRepository.save(pmq);
            }
        }

        // Reload the product to include the updated relationships
        return productRepository.findById(savedProduct.getId()).orElse(savedProduct);
    }

    /**
     * Get all materials that have been allocated to this manufacturer via blockchain
     * and are available to use in production.
     * Only includes materials that have been DELIVERED via transport.
     */
    public List<MaterialDTO> getAvailableMaterialsWithBlockchainIds(Long manufacturerId) {
        try {
            // Find the manufacturer
            Users manufacturer = userRepository.findById(manufacturerId)
                    .orElseThrow(() -> new RuntimeException("Manufacturer not found"));

            // Query ONLY items of type "allocated-material" owned by this manufacturer
            // Items will only be owned by manufacturer after distributor completes delivery
            List<Items> allocatedItems = itemRepository.findByOwner_IdAndItemType(
                    manufacturerId, "allocated-material");

            List<MaterialDTO> availableMaterials = new ArrayList<>();

            for (Items item : allocatedItems) {
                // Get the parent material ID from parentItemIds
                List<Long> parentIds = item.getParentItemIds();

                if (parentIds != null && !parentIds.isEmpty()) {
                    // Find the original material based on blockchain item ID
                    Optional<Material> originalMaterial = materialRepository.findByBlockchainItemId(parentIds.get(0));

                    if (originalMaterial.isPresent()) {
                        Material material = originalMaterial.get();

                        MaterialDTO materialDTO = new MaterialDTO();
                        materialDTO.setId(material.getId());
                        materialDTO.setName(item.getName());
                        materialDTO.setDescription(material.getDescription());
                        materialDTO.setUnit(material.getUnit());
                        materialDTO.setQuantity(item.getQuantity());
                        materialDTO.setBlockchainItemId(item.getId());
                        materialDTO.setItemType(item.getItemType());

                        availableMaterials.add(materialDTO);
                    }
                }
            }

            return availableMaterials;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching allocated materials: " + e.getMessage(), e);
        }
    }

    public List<Material> getAvailableMaterialsForManufacturer(Long manufacturerId) {
        // Find all material requests that have been delivered to this manufacturer
        List<MaterialRequest> deliveredRequests = materialRequestRepository
                .findByManufacturer_IdAndStatus(manufacturerId, "Delivered");

        // Extract all materials from these requests
        Set<Long> materialIds = new HashSet<>();
        for (MaterialRequest request : deliveredRequests) {
            for (MaterialRequestItem item : request.getItems()) {
                materialIds.add(item.getMaterial().getId());
            }
        }

        // Fetch all these materials
        return materialRepository.findAllById(materialIds);
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
            request.setStatus("Requested"); // Using default status if not provided
            request.setRequestedDeliveryDate(requestedDeliveryDate);
            request.setNotes(notes);
            request.setCreatedAt(new Date());
            request.setUpdatedAt(new Date());

            System.out.println("Saving material request");
            MaterialRequest savedRequest = materialRequestRepository.save(request);
            System.out.println("Material request saved with ID: " + savedRequest.getId());

            // Create request items
            List<MaterialRequestItem> requestItems = new ArrayList<>();
            for (MaterialRequestItemCreateDTO item : items) {
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
                    CompletableFuture<String> future = blockchainService.createItem(
                            blockchainItemId,       // A unique ID for this material request
                            supplyChainId,          // The supply chain ID
                            totalQuantity,          // Use total quantity from all items (must be positive)
                            "material-request",     // Type of the item
                            manufacturerId          // User ID of the creator
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
    @Transactional
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
        Order relatedOrder = null;
        if (orderId != null) {
            relatedOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Validate if order contains this product - add defensive coding
            boolean orderContainsProduct = false;
            for (OrderItem item : relatedOrder.getItems()) {
                if (item.getProduct().getId().equals(productId)) {
                    orderContainsProduct = true;
                    break;
                }
            }

            if (!orderContainsProduct) {
                System.err.println("Warning: Order #" + relatedOrder.getOrderNumber() +
                        " does not contain product ID " + productId);
                // Continue anyway, as this might be intentional
            }
        }

        // Validate material items
        for (MaterialBatchItem item : materials) {
            // This is critical - verify ownership in the blockchain before proceeding
            Items blockchainItem = itemRepository.findById(item.getBlockchainItemId())
                    .orElseThrow(() -> new RuntimeException("Blockchain item not found: " + item.getBlockchainItemId()));

            // Check if the item belongs to this manufacturer
            if (!blockchainItem.getOwner().getId().equals(manufacturerId)) {
                throw new RuntimeException("Material with blockchain ID " + item.getBlockchainItemId() +
                        " is not owned by manufacturer " + manufacturerId +
                        ". Actual owner: " + blockchainItem.getOwner().getId());
            }

            // Check if the item is of the right type
            if (!"allocated-material".equals(blockchainItem.getItemType())) {
                throw new RuntimeException("Material with blockchain ID " + item.getBlockchainItemId() +
                        " is not an allocated material. Type: " + blockchainItem.getItemType());
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

        // Store necessary IDs to use in the callback
        final Long savedBatchId = savedBatch.getId();
        final Long savedProductId = product.getId();
        final Long savedOrderId = relatedOrder != null ? relatedOrder.getId() : null;

        // Use blockchain processing to create the product with all materials as parents
        blockchainService.processItem(
                        sourceItemIds,
                        blockchainItemId,
                        inputQuantities,
                        quantity,
                        "manufactured-product",
                        manufacturerId           // User ID of the processor
                )
                .thenAccept(txHash -> {
                    try {
                        // Update batch in a new transaction
                        updateBatchAfterBlockchain(savedBatchId, blockchainItemId, txHash);

                        // Update product quantity in a new transaction
                        updateProductQuantity(savedProductId, quantity);

                        // If this batch is for an order, update the order item status in a new transaction
                        if (savedOrderId != null) {
                            updateOrderStatus(savedOrderId, savedProductId);
                        }

                        System.out.println("Successfully processed blockchain transaction for batch " +
                                savedBatchId + " with hash " + txHash);
                    } catch (Exception e) {
                        System.err.println("Error updating entities after blockchain processing: " + e.getMessage());
                        e.printStackTrace();

                        // Try to mark batch as failed if update methods failed
                        try {
                            markBatchAsFailed(savedBatchId,
                                    "Error updating after blockchain: " + e.getMessage());
                        } catch (Exception ex) {
                            System.err.println("Could not mark batch as failed: " + ex.getMessage());
                        }
                    }
                })
                .exceptionally(ex -> {
                    // Enhanced error handling
                    String errorMessage = ex.getMessage();
                    Throwable rootCause = ex.getCause();
                    String rootCauseMsg = rootCause != null ? rootCause.getMessage() : "Unknown";

                    try {
                        markBatchAsFailed(savedBatchId,
                                "Blockchain transaction failed: " + errorMessage + ". Root cause: " + rootCauseMsg);
                    } catch (Exception e) {
                        System.err.println("Could not mark batch as failed: " + e.getMessage());
                    }

                    // Log detailed error information for debugging
                    System.err.println("Blockchain transaction failed for batch: " + batchNumber);
                    System.err.println("Batch ID: " + savedBatchId);
                    System.err.println("Materials used: " + sourceItemIds);
                    System.err.println("Order ID: " + savedOrderId);
                    System.err.println("Error message: " + errorMessage);
                    System.err.println("Root cause: " + rootCauseMsg);
                    ex.printStackTrace();

                    throw new RuntimeException("Failed to create product on blockchain: " + errorMessage);
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
            blockchainService.updateItemStatus(savedBatch.getBlockchainItemId(), 3, batch.getManufacturer().getId()) // 3 = COMPLETED
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
            blockchainService.updateItemStatus(savedBatch.getBlockchainItemId(), 4, batch.getManufacturer().getId()) // 4 = REJECTED
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

        @Override
        public String toString() {
            return "MaterialBatchItem{" +
                    "materialId=" + materialId +
                    ", blockchainItemId=" + blockchainItemId +
                    ", quantity=" + quantity +
                    '}';
        }
    }

    /**
     * Start production for an order
     */
    public Order startOrderProduction(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify order is in the correct state
        if (!"Requested".equals(order.getStatus())) {
            throw new RuntimeException("Order is not in Requested status");
        }

        // Update order status
        order.setStatus("In Production");
        order.setUpdatedAt(new Date());

        // Update all order items to "In Production" status
        for (OrderItem item : order.getItems()) {
            item.setStatus("In Production");
            orderItemRepository.save(item);
        }

        return orderRepository.save(order);
    }

    public MaterialRequest getMaterialRequestById(Long requestId) {
        return materialRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Material request not found"));
    }

    /**
     * Create a blockchain item for a product and store it in the Items table
     */
    public void createBlockchainItemForProduct(OrderItem item, Product product, Users manufacturer, Chains supplyChain) {
        try {
            // Generate a unique blockchain ID
            Long blockchainItemId = generateUniqueBlockchainId();

            // Create blockchain item for the product
            blockchainService.createItem(
                    blockchainItemId,
                    supplyChain.getId(),
                    item.getQuantity(),
                    "manufactured-product",
                    manufacturer.getId() // Creator is the manufacturer
            ).thenAccept(txHash -> {
                // Update order item with blockchain ID
                item.setBlockchainItemId(blockchainItemId);
                orderItemRepository.save(item);

                // Also create an Items record in the database
                Items productItem = new Items();
                productItem.setId(blockchainItemId);
                productItem.setName(product.getName());
                productItem.setItemType("manufactured-product");
                productItem.setQuantity(item.getQuantity());
                productItem.setOwner(manufacturer);
                productItem.setSupplyChain(supplyChain);
                productItem.setStatus("CREATED");
                productItem.setBlockchainTxHash(txHash);
                productItem.setBlockchainStatus("CONFIRMED");
                productItem.setCreatedAt(new Date());
                productItem.setUpdatedAt(new Date());

                itemRepository.save(productItem);

                System.out.println("Created blockchain record for product: " + product.getName() +
                        " with ID: " + blockchainItemId);
            }).exceptionally(ex -> {
                System.err.println("Failed to create blockchain item for product: " +
                        product.getName() + " - " + ex.getMessage());
                return null;
            });
        } catch (Exception e) {
            System.err.println("Error creating blockchain item for product: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateBatchAfterBlockchain(Long batchId, Long blockchainItemId, String txHash) {
        ProductionBatch batch = productionBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));

        batch.setBlockchainItemId(blockchainItemId);
        batch.setBlockchainTxHash(txHash);
        batch.setStatus("In Production");
        productionBatchRepository.save(batch);

        System.out.println("Updated batch " + batchId + " with blockchain information");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProductQuantity(Long productId, Long quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        product.setAvailableQuantity(product.getAvailableQuantity() + quantity);
        productRepository.save(product);

        System.out.println("Updated product " + productId + " quantity, added " + quantity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateOrderStatus(Long orderId, Long productId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        boolean updatedAnyItem = false;

        for (OrderItem orderItem : order.getItems()) {
            if (orderItem.getProduct().getId().equals(productId)) {
                orderItem.setStatus("In Production");
                orderItemRepository.save(orderItem);
                updatedAnyItem = true;
            }
        }

        if (updatedAnyItem) {
            order.setStatus("In Production");
            orderRepository.save(order);
            System.out.println("Updated order " + orderId + " status to In Production");
        } else {
            System.err.println("Warning: Order " + orderId + " does not contain product " + productId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markBatchAsFailed(Long batchId, String reason) {
        ProductionBatch batch = productionBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));

        batch.setStatus("Failed");
        batch.setNotes(reason);
        productionBatchRepository.save(batch);

        System.out.println("Marked batch " + batchId + " as failed: " + reason);
    }
}