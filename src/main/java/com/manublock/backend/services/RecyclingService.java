package com.manublock.backend.services;

import com.manublock.backend.models.*;
import com.manublock.backend.repositories.*;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecyclingService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChainRepository chainRepository;

    @Autowired
    private TransportRepository transportRepository;

    @Autowired
    private ExtendedBlockchainService blockchainService;

    // ====== CUSTOMER METHODS ======

    /**
     * Mark an item as churned (ready for recycling)
     */
    public Items markItemAsChurned(Long itemId, Long customerId, String notes, String pickupAddress) {
        // Find the item
        Items item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // Verify ownership
        if (!item.getOwner().getId().equals(customerId)) {
            throw new RuntimeException("Customer does not own this item");
        }

        // Verify that the item is not already churned
        if ("CHURNED".equals(item.getStatus()) ||
                "IN_TRANSIT".equals(item.getStatus()) ||
                "RECYCLING_RECEIVED".equals(item.getStatus()) ||
                "RECYCLED".equals(item.getStatus()) ||
                "REFURBISHED".equals(item.getStatus())) {
            throw new RuntimeException("Item is already in the recycling process");
        }

        // Update item status
        item.setStatus("CHURNED");
        item.setUpdatedAt(new Date());

        // Add notes if provided
        if (notes != null && !notes.isEmpty()) {
            item.setNotes(notes);
        }

        // Add pickup address if provided
        if (pickupAddress != null && !pickupAddress.isEmpty()) {
            item.setPickupAddress(pickupAddress);
        }

        // Save to database
        Items savedItem = itemRepository.save(item);

        // Update blockchain status - using status 4 (REJECTED) for CHURNED
        blockchainService.updateItemStatus(itemId, 4, customerId)
                .thenAccept(txHash -> {
                    // Update blockchain transaction hash
                    savedItem.setBlockchainTxHash(txHash);
                    savedItem.setBlockchainStatus("CONFIRMED");
                    itemRepository.save(savedItem);
                })
                .exceptionally(ex -> {
                    System.err.println("Failed to update item status on blockchain: " + ex.getMessage());
                    savedItem.setBlockchainStatus("FAILED");
                    itemRepository.save(savedItem);
                    return null;
                });

        return savedItem;
    }

    /**
     * Get all churned items for a customer
     */
    public List<Items> getChurnedItemsByCustomer(Long customerId) {
        Users customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Find all items owned by this customer with CHURNED status
        return itemRepository.findByOwner_IdAndStatus(customerId, "CHURNED");
    }

    /**
     * Get all recycling transports for a customer
     */
    public List<Transport> getRecyclingTransportsByCustomer(Long customerId) {
        // Find all transports where the customer is the source (recycling pickup)
        // and type is "Recycling Pickup"
        return transportRepository.findBySource_IdAndType(customerId, "Recycling Pickup");
    }

    // ====== DISTRIBUTOR METHODS ======

    /**
     * Create a transport for recycling pickup
     */
    public Transport createRecyclingTransport(
            Long distributorId, Long customerId, Long manufacturerId, Long itemId, Long supplyChainId,
            Date scheduledPickupDate, Date scheduledDeliveryDate, String notes) {

        // Verify entities exist
        Users distributor = userRepository.findById(distributorId)
                .orElseThrow(() -> new RuntimeException("Distributor not found"));

        Users customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Users manufacturer = userRepository.findById(manufacturerId)
                .orElseThrow(() -> new RuntimeException("Manufacturer not found"));

        Chains supplyChain = chainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply chain not found"));

        Items item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // Verify item is churned and owned by customer
        if (!item.getStatus().equals("CHURNED")) {
            throw new RuntimeException("Item is not marked for recycling");
        }

        if (!item.getOwner().getId().equals(customerId)) {
            throw new RuntimeException("Item is not owned by the specified customer");
        }

        // Generate tracking number
        String trackingNumber = "REC-" + Calendar.getInstance().getTimeInMillis();

        // Create transport record
        Transport transport = new Transport();
        transport.setTrackingNumber(trackingNumber);
        transport.setDistributor(distributor);
        transport.setSource(customer);
        transport.setDestination(manufacturer);
        transport.setSupplyChain(supplyChain);
        transport.setType("Recycling Pickup");
        transport.setStatus("Scheduled");
        transport.setScheduledPickupDate(scheduledPickupDate);
        transport.setScheduledDeliveryDate(scheduledDeliveryDate);
        transport.setNotes(notes);
        transport.setCreatedAt(new Date());
        transport.setUpdatedAt(new Date());

        // Direct association with the recycled item
        transport.setRecycledItem(item);

        // Save transport
        return transportRepository.save(transport);
    }

    /**
     * Get all available churned items ready for recycling pickup
     */
    /**
     * Get all available churned items ready for recycling pickup
     */
    public List<Map<String, Object>> getAvailableChurnedItems() {
        // Find all items with CHURNED status
        List<Items> items = itemRepository.findByStatus("CHURNED");

        // Get all recycling transports that are scheduled or in transit
        List<Transport> activeRecyclingTransports = transportRepository.findByTypeAndStatusIn(
                "Recycling Pickup",
                Arrays.asList("Scheduled", "In Transit")
        );

        // Create a set of item IDs that are already scheduled for pickup
        Set<Long> scheduledItemIds = activeRecyclingTransports.stream()
                .map(Transport::getRecycledItem)
                .filter(Objects::nonNull)
                .map(Items::getId)
                .collect(Collectors.toSet());

        System.out.println("Items already scheduled for pickup: " + scheduledItemIds);

        // Filter out items that are already scheduled for pickup
        List<Items> availableItems = items.stream()
                .filter(item -> !scheduledItemIds.contains(item.getId()))
                .collect(Collectors.toList());

        System.out.println("Total churned items: " + items.size());
        System.out.println("Available churned items after filtering: " + availableItems.size());

        List<Map<String, Object>> result = new ArrayList<>();

        for (Items item : availableItems) {
            Map<String, Object> itemInfo = new HashMap<>();
            itemInfo.put("id", item.getId());
            itemInfo.put("name", item.getName());
            itemInfo.put("type", item.getItemType());
            itemInfo.put("quantity", item.getQuantity());
            itemInfo.put("customerId", item.getOwner().getId());
            itemInfo.put("customerName", item.getOwner().getUsername());
            itemInfo.put("supplyChainId", item.getSupplyChain().getId());
            itemInfo.put("supplyChainName", item.getSupplyChain().getName());
            itemInfo.put("pickupAddress", item.getPickupAddress());
            itemInfo.put("updated_at", item.getUpdatedAt());

            // Add manufacturer information
            // First look for products with the same name
            List<Product> matchingProducts = productRepository.findByActiveTrue();
            matchingProducts = matchingProducts.stream()
                    .filter(p -> p.getName().equals(item.getName()))
                    .collect(Collectors.toList());

            if (!matchingProducts.isEmpty()) {
                // Take the first matching product
                Product product = matchingProducts.get(0);
                Users manufacturer = product.getManufacturer();
                itemInfo.put("manufacturerId", manufacturer.getId());
                itemInfo.put("manufacturerName", manufacturer.getUsername());
            } else {
                // If no matching product found, find manufacturers
                List<Users> manufacturers = userRepository.findByRole(Roles.MANUFACTURER);

                if (!manufacturers.isEmpty()) {
                    List<Map<String, Object>> manufacturerList = new ArrayList<>();
                    for (Users manufacturer : manufacturers) {
                        Map<String, Object> mInfo = new HashMap<>();
                        mInfo.put("id", manufacturer.getId());
                        mInfo.put("name", manufacturer.getUsername());
                        manufacturerList.add(mInfo);
                    }
                    itemInfo.put("availableManufacturers", manufacturerList);
                }
            }

            result.add(itemInfo);
        }

        return result;
    }

    /**
     * Record pickup of churned item from customer
     */
    public Transport recordRecyclingPickup(Long transportId) {
        Transport transport = transportRepository.findById(transportId)
                .orElseThrow(() -> new RuntimeException("Transport not found"));

        // Verify transport is for recycling and scheduled
        if (!transport.getType().equals("Recycling Pickup")) {
            throw new RuntimeException("Transport is not for recycling pickup");
        }

        if (!transport.getStatus().equals("Scheduled")) {
            throw new RuntimeException("Transport is not in Scheduled status");
        }

        // Get the directly associated item
        Items item = transport.getRecycledItem();
        if (item == null) {
            System.out.println("No directly associated item found, searching for churned items...");

            // Fallback to find item if no direct association exists
            List<Items> customerChurnedItems = itemRepository.findByOwner_IdAndStatus(
                    transport.getSource().getId(), "CHURNED");

            if (customerChurnedItems.isEmpty()) {
                throw new RuntimeException("No churned items found for this customer");
            }

            item = customerChurnedItems.get(0);
            System.out.println("Found fallback item: " + item.getId() + ", name: " + item.getName());

            // Set association for future use
            transport.setRecycledItem(item);
        } else {
            System.out.println("Using directly associated item: " + item.getId() + ", name: " + item.getName());
        }

        // Update transport status
        transport.setStatus("In Transit");
        transport.setActualPickupDate(new Date());
        transport.setUpdatedAt(new Date());

        // Save changes to transport
        final Transport savedTransport = transportRepository.save(transport);
        System.out.println("Transport updated to In Transit status, ID: " + savedTransport.getId());

        // Capture the item in a final variable for the lambda
        final Items finalItem = item;

        // Update blockchain - transfer from customer to distributor
        Long customerId = transport.getSource().getId();
        Long distributorId = transport.getDistributor().getId();
        Long itemId = finalItem.getId();
        Long quantity = finalItem.getQuantity();

        System.out.println("Starting blockchain transfer from customer " + customerId +
                " to distributor " + distributorId + " for item " + itemId +
                " with quantity " + quantity);

        // Make blockchain call synchronous for debugging
        try {
            String txHash = blockchainService.transferItem(
                    itemId,
                    distributorId,
                    quantity,
                    "recycling-pickup:distributor:" + distributorId,
                    customerId
            ).get(); // This makes it synchronous

            System.out.println("Blockchain transfer completed successfully with hash: " + txHash);

            // Update blockchain transaction hash
            Transport updatedTransport = transportRepository.findById(savedTransport.getId()).orElse(savedTransport);
            updatedTransport.setBlockchainTxHash(txHash);
            transportRepository.save(updatedTransport);
            System.out.println("Updated transport with transaction hash");

            // Update item owner and status - fetch the latest version first
            Items updatedItem = itemRepository.findById(finalItem.getId()).orElse(finalItem);
            updatedItem.setOwner(savedTransport.getDistributor());
            updatedItem.setStatus("IN_TRANSIT");
            updatedItem.setBlockchainTxHash(txHash);
            updatedItem.setBlockchainStatus("CONFIRMED");
            updatedItem.setUpdatedAt(new Date());
            itemRepository.save(updatedItem);
            System.out.println("Updated item ownership and status");

        } catch (Exception ex) {
            System.err.println("BLOCKCHAIN ERROR: Failed to transfer item on blockchain: " + ex.getMessage());
            ex.printStackTrace();
            // Continue without failing the transport operation
        }

        return savedTransport;
    }

    /**
     * Record delivery of churned item to manufacturer
     */
    public Transport recordRecyclingDelivery(Long transportId) {
        Transport transport = transportRepository.findById(transportId)
                .orElseThrow(() -> new RuntimeException("Transport not found"));

        // Verify transport is for recycling and in transit
        if (!transport.getType().equals("Recycling Pickup")) {
            throw new RuntimeException("Transport is not for recycling pickup");
        }

        if (!transport.getStatus().equals("In Transit")) {
            throw new RuntimeException("Transport is not in In Transit status");
        }

        // Get the directly associated item
        Items item = transport.getRecycledItem();
        if (item == null) {
            System.out.println("No directly associated item found, searching for items in transit...");

            // Fallback to find item if no direct association exists
            List<Items> itemsInTransit = itemRepository.findByOwner_IdAndStatus(
                    transport.getDistributor().getId(), "IN_TRANSIT");

            if (itemsInTransit.isEmpty()) {
                throw new RuntimeException("No items in transit found for this distributor");
            }

            item = itemsInTransit.get(0);
            System.out.println("Found fallback item: " + item.getId() + ", name: " + item.getName());

            // Set association for future use
            transport.setRecycledItem(item);
        } else {
            System.out.println("Using directly associated item: " + item.getId() + ", name: " + item.getName());
        }

        // Update transport status
        transport.setStatus("Delivered");
        transport.setActualDeliveryDate(new Date());
        transport.setUpdatedAt(new Date());

        // Save changes to transport
        final Transport savedTransport = transportRepository.save(transport);
        System.out.println("Transport updated to Delivered status, ID: " + savedTransport.getId());

        // Capture the item in a final variable for the lambda
        final Items finalItem = item;

        // Update blockchain - transfer from distributor to manufacturer
        Long distributorId = transport.getDistributor().getId();
        Long manufacturerId = transport.getDestination().getId();
        Long itemId = finalItem.getId();
        Long quantity = finalItem.getQuantity();

        System.out.println("Starting blockchain transfer from distributor " + distributorId +
                " to manufacturer " + manufacturerId + " for item " + itemId +
                " with quantity " + quantity);

        // Make blockchain call synchronous for debugging
        try {
            String txHash = blockchainService.transferItem(
                    itemId,
                    manufacturerId,
                    quantity,
                    "recycling-delivery:manufacturer:" + manufacturerId,
                    distributorId
            ).get(); // This makes it synchronous

            System.out.println("Blockchain transfer completed successfully with hash: " + txHash);

            // Update blockchain transaction hash - fetch the latest version first
            Transport updatedTransport = transportRepository.findById(savedTransport.getId()).orElse(savedTransport);
            updatedTransport.setBlockchainTxHash(txHash);
            transportRepository.save(updatedTransport);
            System.out.println("Updated transport with transaction hash");

            // Update item owner and status - fetch the latest version first
            Items updatedItem = itemRepository.findById(finalItem.getId()).orElse(finalItem);
            updatedItem.setOwner(savedTransport.getDestination());
            updatedItem.setStatus("RECYCLING_RECEIVED");
            updatedItem.setBlockchainTxHash(txHash);
            updatedItem.setBlockchainStatus("CONFIRMED");
            updatedItem.setUpdatedAt(new Date());
            itemRepository.save(updatedItem);
            System.out.println("Updated item ownership and status");

        } catch (Exception ex) {
            System.err.println("BLOCKCHAIN ERROR: Failed to transfer item on blockchain: " + ex.getMessage());
            ex.printStackTrace();
            // Continue without failing the transport operation
        }

        return savedTransport;
    }

    /**
     * Get all recycling transports for a distributor
     */
    public List<Transport> getRecyclingTransportsByDistributor(Long distributorId) {
        return transportRepository.findByDistributor_IdAndType(distributorId, "Recycling Pickup");
    }

    // ====== MANUFACTURER METHODS ======

    /**
     * Get all recycled items waiting for processing by a manufacturer
     */
    public List<Map<String, Object>> getPendingRecycledItems(Long manufacturerId) {
        // Find all items owned by manufacturer with RECYCLING_RECEIVED status
        List<Items> recycledItems = itemRepository.findByOwner_IdAndStatus(manufacturerId, "RECYCLING_RECEIVED");

        List<Map<String, Object>> result = new ArrayList<>();

        for (Items item : recycledItems) {
            Map<String, Object> itemInfo = new HashMap<>();
            itemInfo.put("id", item.getId());
            itemInfo.put("name", item.getName());
            itemInfo.put("type", item.getItemType());
            itemInfo.put("quantity", item.getQuantity());
            itemInfo.put("originalSupplyChainId", item.getSupplyChain().getId());
            itemInfo.put("originalSupplyChainName", item.getSupplyChain().getName());
            itemInfo.put("receivedAt", item.getUpdatedAt());

            result.add(itemInfo);
        }

        return result;
    }

    /**
     * Process a recycled product into materials
     */
    @Transactional
    public List<Material> processToMaterials(
            Long manufacturerId, Long itemId, Long supplyChainId, List<Map<String, Object>> materials) {

        // Verify entities exist
        Users manufacturer = userRepository.findById(manufacturerId)
                .orElseThrow(() -> new RuntimeException("Manufacturer not found"));

        Items recycledItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        Chains supplyChain = chainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply chain not found"));

        // Verify item is owned by manufacturer and has correct status
        if (!recycledItem.getOwner().getId().equals(manufacturerId)) {
            throw new RuntimeException("Item is not owned by the manufacturer");
        }

        if (!recycledItem.getStatus().equals("RECYCLING_RECEIVED")) {
            throw new RuntimeException("Item is not in the correct status for recycling");
        }

        // Process each material
        List<Material> recycledMaterials = new ArrayList<>();
        List<Long> newMaterialBlockchainIds = new ArrayList<>();
        List<Long> outputQuantities = new ArrayList<>();

        for (Map<String, Object> materialData : materials) {
            String name = (String) materialData.get("name");
            String description = (String) materialData.get("description");
            Long quantity = Long.valueOf(materialData.get("quantity").toString());
            String unit = (String) materialData.get("unit");
            String specifications = (String) materialData.get("specifications");

            // Generate blockchain ID for this new material
            Long blockchainItemId = generateUniqueBlockchainId();

            // Create material in database
            Material material = new Material();
            material.setName(name);
            material.setDescription(description);
            material.setQuantity(quantity);
            material.setUnit(unit);
            material.setSpecifications(specifications);
            material.setSupplier(manufacturer); // Manufacturer becomes the supplier of recycled materials
            material.setActive(true);
            material.setBlockchainItemId(blockchainItemId);
            material.setCreatedAt(new Date());
            material.setUpdatedAt(new Date());

            Material savedMaterial = materialRepository.save(material);
            recycledMaterials.add(savedMaterial);

            // Track blockchain IDs and quantities for blockchain processing
            newMaterialBlockchainIds.add(blockchainItemId);
            outputQuantities.add(quantity);
        }

        // Process on blockchain - turn recycled item into multiple materials
        // We're using the processItem function to record the transformation
        blockchainService.processItem(
                Collections.singletonList(itemId),  // Source item is the recycled product
                newMaterialBlockchainIds.get(0),    // Just need one ID for the transaction record
                Collections.singletonList(recycledItem.getQuantity()),  // Input quantity
                outputQuantities.get(0),            // Output quantity (of first material)
                "recycled-material",
                manufacturerId
        ).thenAccept(txHash -> {
            // Update recycled item status
            recycledItem.setStatus("RECYCLED");
            recycledItem.setBlockchainTxHash(txHash);
            recycledItem.setBlockchainStatus("CONFIRMED");
            recycledItem.setUpdatedAt(new Date());
            itemRepository.save(recycledItem);

            // Update all materials with blockchain transaction
            for (Material material : recycledMaterials) {
                material.setBlockchainItemId(material.getBlockchainItemId());
                materialRepository.save(material);
            }
        }).exceptionally(ex -> {
            System.err.println("Failed to process recycled item on blockchain: " + ex.getMessage());
            return null;
        });

        return recycledMaterials;
    }

    /**
     * Refurbish a recycled product for resale
     */
    @Transactional
    public Product refurbishProduct(Long manufacturerId, Long itemId, String quality, String notes) {
        // Verify entities exist
        Users manufacturer = userRepository.findById(manufacturerId)
                .orElseThrow(() -> new RuntimeException("Manufacturer not found"));

        Items recycledItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // Verify item is owned by manufacturer and has correct status
        if (!recycledItem.getOwner().getId().equals(manufacturerId)) {
            throw new RuntimeException("Item is not owned by the manufacturer");
        }

        if (!recycledItem.getStatus().equals("RECYCLING_RECEIVED")) {
            throw new RuntimeException("Item is not in the correct status for refurbishing");
        }

        // Find original product associated with this item (assuming it's a manufactured product)
        // This would require additional data linking or querying
        // For this example, we'll create a new product with similar properties

        // Generate blockchain ID for refurbished product
        Long refurbishedBlockchainId = generateUniqueBlockchainId();

        // Create or update product in database
        Product product = new Product();
        product.setName(recycledItem.getName() + " (Refurbished)");
        product.setDescription("Refurbished product. Quality: " + quality);
        product.setSpecifications(notes);
        product.setSku("REFURB-" + Calendar.getInstance().getTimeInMillis());
        product.setAvailableQuantity(recycledItem.getQuantity());
        product.setManufacturer(manufacturer);
        product.setActive(true);
        product.setBlockchainItemId(refurbishedBlockchainId);
        product.setCreatedAt(new Date());
        product.setUpdatedAt(new Date());

        Product savedProduct = productRepository.save(product);

        // Process on blockchain - refurbish recycled item into new product
        blockchainService.processItem(
                Collections.singletonList(itemId),  // Source item is the recycled product
                refurbishedBlockchainId,            // New blockchain ID for refurbished product
                Collections.singletonList(recycledItem.getQuantity()),  // Input quantity
                recycledItem.getQuantity(),         // Same output quantity
                "refurbished-product",
                manufacturerId
        ).thenAccept(txHash -> {
            // Update recycled item status
            recycledItem.setStatus("REFURBISHED");
            recycledItem.setBlockchainTxHash(txHash);
            recycledItem.setBlockchainStatus("CONFIRMED");
            recycledItem.setUpdatedAt(new Date());
            itemRepository.save(recycledItem);

            // Update product with blockchain ID
            savedProduct.setBlockchainItemId(refurbishedBlockchainId);
            productRepository.save(savedProduct);
        }).exceptionally(ex -> {
            System.err.println("Failed to process refurbished item on blockchain: " + ex.getMessage());
            return null;
        });

        return savedProduct;
    }

    /**
     * Get all recycled materials for a manufacturer
     */
    public List<Material> getRecycledMaterialsByManufacturer(Long manufacturerId) {
        // Here we would need to differentiate between normal materials and recycled ones
        // This might require adding a "source" or "type" field to the Material entity
        // For now, we'll just return all materials where the manufacturer is the supplier
        return materialRepository.findBySupplier_Id(manufacturerId);
    }

    /**
     * Get all refurbished products for a manufacturer
     */
    public List<Product> getRefurbishedProductsByManufacturer(Long manufacturerId) {
        // Similar to recycled materials, we'd need a way to identify refurbished products
        // This might require adding a "source" or "type" field to the Product entity
        // For now, we'll just filter by name containing "Refurbished"
        List<Product> allProducts = productRepository.findByManufacturer_Id(manufacturerId);
        return allProducts.stream()
                .filter(p -> p.getName() != null && p.getName().contains("Refurbished"))
                .collect(Collectors.toList());
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
}