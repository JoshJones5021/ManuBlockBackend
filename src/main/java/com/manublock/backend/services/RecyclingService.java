package com.manublock.backend.services;

import com.manublock.backend.models.*;
import com.manublock.backend.repositories.*;
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

    @Autowired
    private OrderItemRepository orderItemRepository;

    public Items markItemAsChurned(Long itemId, Long customerId, String notes, String pickupAddress) {
        Items item = itemRepository.findById(itemId).orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getOwner().getId().equals(customerId)) {
            throw new RuntimeException("Customer does not own this item");
        }

        // Check ALL recycling-related statuses, not just CHURNED
        if (Arrays.asList("CHURNED", "IN_TRANSIT", "RECYCLING_RECEIVED", "RECYCLED",
                "RECYCLING_IN_TRANSIT", "SCHEDULED_FOR_RECYCLING").contains(item.getStatus())) {
            throw new RuntimeException("Item is already in the recycling process");
        }

        item.setStatus("CHURNED");
        item.setUpdatedAt(new Date());
        if (notes != null) item.setNotes(notes);
        if (pickupAddress != null) item.setPickupAddress(pickupAddress);
        Items savedItem = itemRepository.save(item);

        blockchainService.updateItemStatus(itemId, 4, customerId).thenAccept(txHash -> {
            savedItem.setBlockchainTxHash(txHash);
            savedItem.setBlockchainStatus("CONFIRMED");
            itemRepository.save(savedItem);
        }).exceptionally(ex -> {
            savedItem.setBlockchainStatus("FAILED");
            itemRepository.save(savedItem);
            return null;
        });

        return savedItem;
    }

    // CUSTOMER: Get churned items
    public List<Items> getChurnedItemsByCustomer(Long customerId) {
        userRepository.findById(customerId).orElseThrow(() -> new RuntimeException("Customer not found"));
        return itemRepository.findByOwner_IdAndStatus(customerId, "CHURNED");
    }

    // DISTRIBUTOR: Create transport for recycling pickup
    @Transactional
    public Transport createRecyclingTransport(Long distributorId, Long customerId, Long manufacturerId, Long itemId, Long supplyChainId,
                                              Date scheduledPickupDate, Date scheduledDeliveryDate, String notes) {

        Users distributor = userRepository.findById(distributorId).orElseThrow(() -> new RuntimeException("Distributor not found"));
        Users customer = userRepository.findById(customerId).orElseThrow(() -> new RuntimeException("Customer not found"));
        Users manufacturer = userRepository.findById(manufacturerId).orElseThrow(() -> new RuntimeException("Manufacturer not found"));
        Chains supplyChain = chainRepository.findById(supplyChainId).orElseThrow(() -> new RuntimeException("Supply chain not found"));
        Items item = itemRepository.findById(itemId).orElseThrow(() -> new RuntimeException("Item not found"));

        if (!"CHURNED".equals(item.getStatus()) || !item.getOwner().getId().equals(customerId)) {
            throw new RuntimeException("Invalid item status or ownership");
        }

        Transport transport = new Transport();
        transport.setTrackingNumber("REC-" + Calendar.getInstance().getTimeInMillis());
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
        transport.setRecycledItem(item);

        // Save the transport
        Transport savedTransport = transportRepository.save(transport);

        // Update the item status
        item.setStatus("SCHEDULED_FOR_RECYCLING");  // Or any other status name you prefer
        item.setUpdatedAt(new Date());
        itemRepository.save(item);

        return savedTransport;
    }

    // DISTRIBUTOR: Record pickup of churned item
    @Transactional
    public Transport recordRecyclingPickup(Long transportId) {
        Transport transport = transportRepository.findById(transportId).orElseThrow(() -> new RuntimeException("Transport not found"));
        if (!"Scheduled".equals(transport.getStatus())) throw new RuntimeException("Transport is not Scheduled");

        Items item = transport.getRecycledItem();
        transport.setStatus("In Transit");
        transport.setActualPickupDate(new Date());
        transport.setUpdatedAt(new Date());
        transportRepository.save(transport);

        blockchainService.transferItem(item.getId(), transport.getDistributor().getId(), item.getQuantity(),
                        "recycling-pickup:distributor:" + transport.getDistributor().getId(), item.getOwner().getId())
                .thenAccept(txHash -> {
                    transport.setBlockchainTxHash(txHash);
                    transportRepository.save(transport);
                    item.setOwner(transport.getDistributor());
                    item.setStatus("RECYCLING_IN_TRANSIT");
                    item.setBlockchainTxHash(txHash);
                    item.setBlockchainStatus("CONFIRMED");
                    item.setUpdatedAt(new Date());
                    itemRepository.save(item);
                });

        return transport;
    }

    // DISTRIBUTOR: Record delivery of item to manufacturer
    @Transactional
    public Transport recordRecyclingDelivery(Long transportId) {
        Transport transport = transportRepository.findById(transportId).orElseThrow(() -> new RuntimeException("Transport not found"));
        if (!"In Transit".equals(transport.getStatus())) throw new RuntimeException("Transport is not In Transit");

        Items item = transport.getRecycledItem();
        transport.setStatus("Delivered");
        transport.setActualDeliveryDate(new Date());
        transport.setUpdatedAt(new Date());
        transportRepository.save(transport);

        blockchainService.transferItem(item.getId(), transport.getDestination().getId(), item.getQuantity(),
                "recycling-delivery:manufacturer:" + transport.getDestination().getId(),
                transport.getDistributor().getId()).thenAccept(txHash -> {
            transport.setBlockchainTxHash(txHash);
            transportRepository.save(transport);
            item.setOwner(transport.getDestination());
            item.setStatus("RECYCLING_RECEIVED");
            item.setBlockchainTxHash(txHash);
            item.setBlockchainStatus("CONFIRMED");
            item.setUpdatedAt(new Date());
            itemRepository.save(item);
        });

        return transport;
    }

    // MANUFACTURER: Get pending recycled items
    public List<Map<String, Object>> getPendingRecycledItems(Long manufacturerId) {
        List<Items> recycledItems = itemRepository.findByOwner_IdAndStatus(manufacturerId, "RECYCLING_RECEIVED");

        List<Map<String, Object>> result = new ArrayList<>();
        for (Items item : recycledItems) {
            Map<String, Object> itemInfo = new HashMap<>();
            itemInfo.put("id", item.getId());
            itemInfo.put("productName", item.getName());
            itemInfo.put("productType", item.getItemType());
            itemInfo.put("quantity", item.getQuantity());
            itemInfo.put("originalSupplyChainId", item.getSupplyChain().getId());
            itemInfo.put("originalSupplyChainName", item.getSupplyChain().getName());
            itemInfo.put("receivedDate", item.getUpdatedAt());
            itemInfo.put("status", item.getStatus());

            orderItemRepository.findByBlockchainItemId(item.getId()).ifPresent(orderItem ->
                    itemInfo.put("customerName", orderItem.getOrder().getCustomer().getUsername()));

            result.add(itemInfo);
        }
        return result;
    }

    /**
     * Process recycled product into materials and add directly to manufacturer's inventory
     */
    @Transactional
    public List<Material> processToMaterials(Long manufacturerId, Long itemId, Long supplyChainId, List<Map<String, Object>> materials) {
        Users manufacturer = userRepository.findById(manufacturerId)
                .orElseThrow(() -> new RuntimeException("Manufacturer not found"));
        Items recycledItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        Chains supplyChain = chainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply chain not found"));

        if (!recycledItem.getOwner().getId().equals(manufacturerId) || !"RECYCLING_RECEIVED".equals(recycledItem.getStatus())) {
            throw new RuntimeException("Invalid recycled item state");
        }

        // Calculate total material quantity being salvaged
        double totalSalvagedQuantity = 0;
        for (Map<String, Object> materialData : materials) {
            Long quantity = Long.parseLong(materialData.get("quantity").toString());
            totalSalvagedQuantity += quantity;
        }

        // Validate against original product's total material
        Optional<OrderItem> orderItemOpt = orderItemRepository.findByBlockchainItemId(itemId);
        if (orderItemOpt.isPresent()) {
            OrderItem orderItem = orderItemOpt.get();
            Product product = orderItem.getProduct();

            double originalMaterialQuantity = 0;
            if (product.getMaterialQuantities() != null && !product.getMaterialQuantities().isEmpty()) {
                for (ProductMaterialQuantity pmq : product.getMaterialQuantities()) {
                    originalMaterialQuantity += pmq.getQuantity();
                }

                double maxRecoverableQuantity = originalMaterialQuantity;
                if (totalSalvagedQuantity > maxRecoverableQuantity) {
                    throw new RuntimeException("Cannot recover more materials than what was originally used to make the product. Maximum recoverable: " + maxRecoverableQuantity);
                }
            }
        }

        List<Material> recycledMaterials = new ArrayList<>();
        List<Long> materialBlockchainIds = new ArrayList<>();
        List<Long> outputQuantities = new ArrayList<>();

        // Core logic to update or create materials
        for (Map<String, Object> materialData : materials) {
            String materialName = (String) materialData.get("name");
            Long quantity = Long.parseLong(materialData.get("quantity").toString());

            if (quantity <= 0) continue; // Skip zero quantity

            List<Material> existingMaterials = materialRepository.findByNameAndSupplier_Id(materialName, manufacturerId);
            Material material;

            if (!existingMaterials.isEmpty()) {
                material = existingMaterials.get(0);
                material.setQuantity(material.getQuantity() + quantity);
                material.setUpdatedAt(new Date());

                // Update additional properties if provided
                material.setDescription((String) materialData.get("description"));
                material.setUnit((String) materialData.get("unit"));
                material.setSpecifications((String) materialData.get("specifications"));
            } else {
                material = new Material();
                material.setName(materialName);
                material.setDescription((String) materialData.get("description"));
                material.setQuantity(quantity);
                material.setUnit((String) materialData.get("unit"));
                material.setSpecifications((String) materialData.get("specifications"));
                material.setSupplier(manufacturer);
                material.setActive(true);
                material.setBlockchainItemId(generateUniqueBlockchainId());
                material.setCreatedAt(new Date());
                material.setUpdatedAt(new Date());
            }

            recycledMaterials.add(materialRepository.save(material));
            materialBlockchainIds.add(material.getBlockchainItemId());
            outputQuantities.add(quantity);
        }

        // Blockchain processing
        if (!materialBlockchainIds.isEmpty()) {
            Long primaryMaterialBlockchainId = materialBlockchainIds.get(0);
            Long primaryOutputQuantity = outputQuantities.get(0);

            blockchainService.processItem(
                            Collections.singletonList(recycledItem.getId()),
                            primaryMaterialBlockchainId,
                            Collections.singletonList(recycledItem.getQuantity()),
                            primaryOutputQuantity,
                            "recycled-material",
                            manufacturerId
                    )
                    .thenAccept(txHash -> {
                        recycledItem.setStatus("RECYCLED");
                        recycledItem.setBlockchainTxHash(txHash);
                        recycledItem.setBlockchainStatus("CONFIRMED");
                        recycledItem.setUpdatedAt(new Date());
                        itemRepository.save(recycledItem);

                        // Optional: Update status of related material items if they exist
                        try {
                            for (Long materialId : materialBlockchainIds) {
                                Optional<Items> materialItemOpt = itemRepository.findById(materialId);
                                if (materialItemOpt.isPresent()) {
                                    Items materialItem = materialItemOpt.get();
                                    materialItem.setStatus("AVAILABLE");
                                    materialItem.setUpdatedAt(new Date());
                                    itemRepository.save(materialItem);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error updating recycled material status: " + e.getMessage());
                        }
                    });
        } else {
            // No valid materials, just mark recycled item as recycled
            recycledItem.setStatus("RECYCLED");
            recycledItem.setUpdatedAt(new Date());
            itemRepository.save(recycledItem);
        }

        return recycledMaterials;
    }


    // MANUFACTURER: Get recycled materials
    public List<Material> getRecycledMaterialsByManufacturer(Long manufacturerId) {
        return materialRepository.findBySupplier_Id(manufacturerId);
    }

    private Long generateUniqueBlockchainId() {
        return new Random().nextLong(1_000_000_000L, 9_999_999_999L);
    }

    public List<Transport> getRecyclingTransportsByCustomer(Long customerId) {
        return transportRepository.findBySource_IdAndType(customerId, "Recycling Pickup");
    }

    public List<Map<String, Object>> getAvailableChurnedItems() {
        return itemRepository.findByStatus("CHURNED")
                .stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", item.getId());
                    map.put("name", item.getName());
                    map.put("status", item.getStatus());
                    map.put("customerName", item.getOwner().getUsername());
                    map.put("pickupAddress", item.getPickupAddress());
                    map.put("updated_at", item.getUpdatedAt());
                    map.put("customerId", item.getOwner().getId());
                    map.put("supplyChainId", item.getSupplyChain().getId());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Transport> getRecyclingTransportsByDistributor(Long distributorId) {
        return transportRepository.findByDestination_IdAndType(distributorId, "Recycling Delivery");
    }
}
