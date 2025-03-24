package com.manublock.backend.controllers;

import com.manublock.backend.dto.*;
import com.manublock.backend.models.*;
import com.manublock.backend.repositories.*;
import com.manublock.backend.services.ManufacturerService;
import com.manublock.backend.utils.CustomException;
import com.manublock.backend.utils.DTOConverter;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manufacturer")
public class ManufacturerController {

    private final ManufacturerService manufacturerService;

    @Autowired
    public ManufacturerController(ManufacturerService manufacturerService) {
        this.manufacturerService = manufacturerService;
    }

    @Autowired
    private CustomerController customerController;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransportRepository transportRepository;

    @Autowired
    private ChainRepository chainRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody Map<String, Object> payload) {
        try {
            String name = (String) payload.get("name");
            String description = (String) payload.get("description");
            String specifications = (String) payload.get("specifications");
            String sku = (String) payload.get("sku");
            BigDecimal price = new BigDecimal(payload.get("price").toString());
            Long manufacturerId = Long.valueOf(payload.get("manufacturerId").toString());
            Long supplyChainId = Long.valueOf(payload.get("supplyChainId").toString());

            // Extract materials with quantities
            List<MaterialQuantityDTO> materialQuantities = new ArrayList<>();
            if (payload.containsKey("requiredMaterials")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> materialsList = (List<Map<String, Object>>) payload.get("requiredMaterials");

                for (Map<String, Object> materialData : materialsList) {
                    MaterialQuantityDTO dto = new MaterialQuantityDTO();
                    dto.setMaterialId(Long.valueOf(materialData.get("materialId").toString()));
                    dto.setQuantity(Long.valueOf(materialData.get("quantity").toString()));
                    materialQuantities.add(dto);
                }
            }

            Product product = manufacturerService.createProduct(
                    name, description, specifications, sku, price,
                    manufacturerId, supplyChainId, materialQuantities);

            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating product: " + e.getMessage());
        }
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> payload) {
        try {
            String name = (String) payload.get("name");
            String description = (String) payload.get("description");
            String specifications = (String) payload.get("specifications");
            String sku = (String) payload.get("sku");
            BigDecimal price = new BigDecimal(payload.get("price").toString());

            // Extract materials with quantities
            List<MaterialQuantityDTO> materialQuantities = new ArrayList<>();
            if (payload.containsKey("requiredMaterials")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> materialsList = (List<Map<String, Object>>) payload.get("requiredMaterials");

                for (Map<String, Object> materialData : materialsList) {
                    MaterialQuantityDTO dto = new MaterialQuantityDTO();
                    dto.setMaterialId(Long.valueOf(materialData.get("materialId").toString()));
                    dto.setQuantity(Long.valueOf(materialData.get("quantity").toString()));
                    materialQuantities.add(dto);
                }
            }

            Product product = manufacturerService.updateProduct(
                    productId, name, description, specifications, sku, price, materialQuantities);

            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating product: " + e.getMessage());
        }
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<?> deactivateProduct(@PathVariable Long productId) {
        try {
            Product product = manufacturerService.deactivateProduct(productId);
            // Convert to DTO
            ProductDTO productDTO = ProductDTO.fromEntity(product);
            return ResponseEntity.ok(productDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deactivating product: " + e.getMessage());
        }
    }

    @PostMapping("/materials/request")
    public ResponseEntity<?> requestMaterials(@RequestBody MaterialRequestCreateDTO requestDTO) {
        try {
            MaterialRequest result = manufacturerService.requestMaterials(
                    requestDTO.getManufacturerId(),
                    requestDTO.getSupplierId(),
                    requestDTO.getSupplyChainId(),
                    requestDTO.getItems(),
                    requestDTO.getRequestedDeliveryDate(),
                    requestDTO.getNotes()
            );
            return ResponseEntity.ok(new MaterialRequestDTO(result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating material request: " + e.getMessage());
        }
    }

    // Add this to the beginning of the createProductionBatch method in ManufacturerController.java
    @PostMapping("/production/batch")
    public ResponseEntity<?> createProductionBatch(@RequestBody Map<String, Object> payload) {
        try {
            Long manufacturerId = Long.valueOf(payload.get("manufacturerId").toString());
            Long productId = Long.valueOf(payload.get("productId").toString());
            Long supplyChainId = Long.valueOf(payload.get("supplyChainId").toString());
            Long orderId = payload.get("orderId") != null ?
                    Long.valueOf(payload.get("orderId").toString()) : null;
            Long quantity = Long.valueOf(payload.get("quantity").toString());

            // Handle the materials array properly
            List<ManufacturerService.MaterialBatchItem> materials = new ArrayList<>();

            if (payload.get("materials") instanceof List) {
                List<?> materialsList = (List<?>) payload.get("materials");

                for (Object rawItem : materialsList) {
                    if (rawItem instanceof Map) {
                        Map<?, ?> itemMap = (Map<?, ?>) rawItem;

                        // Create a new MaterialBatchItem for each entry
                        ManufacturerService.MaterialBatchItem item = new ManufacturerService.MaterialBatchItem();

                        if (itemMap.get("materialId") != null) {
                            item.setMaterialId(Long.valueOf(itemMap.get("materialId").toString()));
                        }

                        if (itemMap.get("blockchainItemId") != null) {
                            item.setBlockchainItemId(Long.valueOf(itemMap.get("blockchainItemId").toString()));
                        }

                        if (itemMap.get("quantity") != null) {
                            item.setQuantity(Long.valueOf(itemMap.get("quantity").toString()));
                        }

                        materials.add(item);
                    }
                }
            }

            // Call the service to create the production batch
            ProductionBatch batch = manufacturerService.createProductionBatch(
                    manufacturerId, productId, supplyChainId, orderId, quantity, materials);

            // Convert to DTO before returning
            ProductionBatchDTO batchDTO = new ProductionBatchDTO(batch);
            return ResponseEntity.ok(batchDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating production batch: " + e.getMessage());
        }
    }

    @PostMapping("/production/batch/{batchId}/complete")
    public ResponseEntity<?> completeProductionBatch(
            @PathVariable Long batchId,
            @RequestBody Map<String, String> payload) {
        try {
            String quality = payload.get("quality");

            ProductionBatch batch = manufacturerService.completeProductionBatch(batchId, quality);
            // Convert to DTO
            ProductionBatchDTO batchDTO = new ProductionBatchDTO(batch);
            return ResponseEntity.ok(batchDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error completing production batch: " + e.getMessage());
        }
    }

    @PostMapping("/production/batch/{batchId}/reject")
    public ResponseEntity<?> rejectProductionBatch(
            @PathVariable Long batchId,
            @RequestBody Map<String, String> payload) {
        try {
            String reason = payload.get("reason");

            ProductionBatch batch = manufacturerService.rejectProductionBatch(batchId, reason);
            // Convert to DTO
            ProductionBatchDTO batchDTO = new ProductionBatchDTO(batch);
            return ResponseEntity.ok(batchDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error rejecting production batch: " + e.getMessage());
        }
    }

    @GetMapping("/products/{manufacturerId}")
    public ResponseEntity<List<ProductDTO>> getProductsByManufacturer(@PathVariable Long manufacturerId) {
        List<Product> products = manufacturerService.getProductsByManufacturer(manufacturerId);
        List<ProductDTO> productDTOs = products.stream()
                .map(ProductDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(productDTOs);
    }

    @GetMapping("/materials/available/{manufacturerId}")
    public ResponseEntity<?> getAvailableMaterials(@PathVariable Long manufacturerId) {
        try {
            // Get materials from supplier allocation
            List<Material> purchasedMaterials = manufacturerService.getAvailableMaterialsForManufacturer(manufacturerId);

            // Also get materials from recycling
            List<Material> recycledMaterials = materialRepository.findBySupplier_Id(manufacturerId);

            // Combine the lists, removing duplicates by material ID
            Map<Long, Material> combinedMaterials = new HashMap<>();

            // Add purchased materials
            for (Material material : purchasedMaterials) {
                combinedMaterials.put(material.getId(), material);
            }

            // Add recycled materials (will overwrite duplicates)
            for (Material material : recycledMaterials) {
                combinedMaterials.put(material.getId(), material);
            }

            return ResponseEntity.ok(new ArrayList<>(combinedMaterials.values()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving available materials: " + e.getMessage()));
        }
    }

    @GetMapping("/materials/requests/{manufacturerId}")
    public ResponseEntity<?> getRequestsByManufacturer(@PathVariable Long manufacturerId) {
        try {
            // Get the material requests from the service
            List<MaterialRequest> requests = manufacturerService.getRequestsByManufacturer(manufacturerId);

            // Convert to DTOs to prevent circular references
            List<MaterialRequestDTO> requestDTOs = requests.stream()
                    .map(MaterialRequestDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(requestDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving material requests: " + e.getMessage());
        }
    }

    @GetMapping("/production/batches/{manufacturerId}")
    public ResponseEntity<?> getBatchesByManufacturer(@PathVariable Long manufacturerId) {
        try {
            List<ProductionBatch> batches = manufacturerService.getBatchesByManufacturer(manufacturerId);

            // Convert to DTOs to prevent recursion
            List<ProductionBatchDTO> batchDTOs = batches.stream()
                    .map(ProductionBatchDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(batchDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving production batches: " + e.getMessage());
        }
    }

    @GetMapping("/orders/{manufacturerId}")
    public ResponseEntity<?> getOrdersByManufacturer(@PathVariable Long manufacturerId) {
        try {
            List<Order> orders = manufacturerService.getOrdersByManufacturer(manufacturerId);

            // Convert to DTOs using the enhanced converter
            List<OrderResponseDTO> orderDTOs = DTOConverter.convertToOrderDTOList(orders);

            return ResponseEntity.ok(orderDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving orders: " + e.getMessage());
        }
    }

    @GetMapping("/materials/available-blockchain/{manufacturerId}")
    public ResponseEntity<?> getAvailableMaterialsWithBlockchainIds(@PathVariable Long manufacturerId) {
        try {
            List<MaterialDTO> materials = manufacturerService.getAvailableMaterialsWithBlockchainIds(manufacturerId);
            return ResponseEntity.ok(materials);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving available materials: " + e.getMessage());
        }
    }

    @PostMapping("/orders/{id}/fulfill-from-stock")
    public ResponseEntity<?> fulfillOrderFromStock(@PathVariable Long id, @RequestBody ProductTransportRequestDTO request) {
        try {
            // Find the order
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new CustomException("Order not found"));

            // Verify all products have sufficient inventory
            boolean sufficientInventory = true;
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product.getAvailableQuantity() < item.getQuantity()) {
                    sufficientInventory = false;
                    break;
                }
            }

            if (!sufficientInventory) {
                return ResponseEntity.badRequest().body(Map.of("error", "Insufficient inventory for some items"));
            }

            // Update product quantities
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setAvailableQuantity(product.getAvailableQuantity() - item.getQuantity());
                productRepository.save(product);

                // Update item status
                item.setStatus("Ready for Shipment");
            }

            // Update order status
            order.setStatus("Ready for Shipment");
            Order updatedOrder = orderRepository.save(order);  // Save and get the updated order

            // Set entities
            Users manufacturer = userRepository.findById(request.getManufacturerId())
                    .orElseThrow(() -> new CustomException("Manufacturer not found"));
            Users customer = userRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new CustomException("Customer not found"));
            Users distributor = userRepository.findById(request.getDistributorId())
                    .orElseThrow(() -> new CustomException("Distributor not found"));
            Chains supplyChain = chainRepository.findById(request.getSupplyChainId())
                    .orElseThrow(() -> new CustomException("Supply chain not found"));

            // Create blockchain items for products using the manufacturerService
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();

                // Check if blockchain ID is already present
                if (item.getBlockchainItemId() == null) {
                    // Call the service method to handle blockchain item creation
                    manufacturerService.createBlockchainItemForProduct(
                            item,
                            product,
                            manufacturer,
                            supplyChain
                    );
                }
            }

            // Create transport entry
            Transport transport = new Transport();
            transport.setTrackingNumber("TR-" + System.currentTimeMillis());
            transport.setType("Product Delivery");
            transport.setStatus("Scheduled");
            transport.setDistributor(distributor);
            transport.setSource(manufacturer);
            transport.setDestination(customer);
            transport.setOrder(updatedOrder);
            transport.setSupplyChain(supplyChain);
            transport.setScheduledPickupDate(request.getScheduledPickupDate());  // Add this line
            transport.setScheduledDeliveryDate(request.getScheduledDeliveryDate());
            transport.setNotes(request.getNotes());

            Transport savedTransport = transportRepository.save(transport);

            // Create DTO for response using the updated order
            OrderResponseDTO orderDTO = DTOConverter.convertToOrderDTO(updatedOrder);
            TransportDTO transportDTO = new TransportDTO(savedTransport);

            return ResponseEntity.ok(Map.of(
                    "order", orderDTO,
                    "transport", transportDTO,
                    "message", "Order fulfilled from stock and ready for shipment"
            ));
        } catch (Exception e) {
            e.printStackTrace(); // Log the full exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fulfill order: " + e.getMessage()));
        }
    }

    @GetMapping("/materials/request/{requestId}")
    public ResponseEntity<?> getMaterialRequestById(@PathVariable Long requestId) {
        try {
            // Fetch the material request by ID
            MaterialRequest materialRequest = manufacturerService.getMaterialRequestById(requestId);

            // Convert to DTO to prevent circular references
            MaterialRequestDTO requestDTO = new MaterialRequestDTO(materialRequest);

            return ResponseEntity.ok(requestDTO);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Material request not found with ID: " + requestId);
        } catch (Exception e) {
            // Log the error for server-side tracking
            System.err.println("Error fetching material request: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving material request: " + e.getMessage());
        }
    }

    @GetMapping("/product/{productId}/materials")
    public ResponseEntity<?> getProductMaterials(@PathVariable Long productId) {
        try {
            // First check if the item exists
            Optional<Items> itemOpt = itemRepository.findById(productId);
            if (!itemOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Product not found with ID: " + productId));
            }

            Items item = itemOpt.get();
            Users manufacturer = item.getOwner();

            // Get materials associated with this manufacturer
            List<Material> materialsList = materialRepository.findByActiveTrueAndSupplier_Id(manufacturer.getId());

            // If that's empty, try using the supply chain ID instead
            if (materialsList.isEmpty() && item.getSupplyChain() != null) {
                Long supplyChainId = item.getSupplyChain().getId();
                materialsList = materialRepository.findByActiveTrueAndSupplier_Id(supplyChainId);
            }

            // If still no materials found, return standard recyclable materials
            if (materialsList.isEmpty()) {
                List<Map<String, Object>> commonMaterials = new ArrayList<>();
                commonMaterials.add(createMaterialInfo("Aluminum", "Recycled aluminum", "kg"));
                commonMaterials.add(createMaterialInfo("Plastic", "Recycled plastic", "kg"));
                commonMaterials.add(createMaterialInfo("Glass", "Recycled glass", "kg"));
                commonMaterials.add(createMaterialInfo("Circuit Board", "Recycled electronics", "kg"));
                commonMaterials.add(createMaterialInfo("Copper Wire", "Recycled copper", "kg"));
                return ResponseEntity.ok(commonMaterials);
            }

            // Convert to Maps for the response
            List<Map<String, Object>> materialInfoList = materialsList.stream()
                    .map(material -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("id", material.getId());
                        info.put("name", material.getName());
                        info.put("description", material.getDescription() != null ?
                                material.getDescription() : "Recycled " + material.getName());
                        info.put("unit", material.getUnit() != null ? material.getUnit() : "kg");
                        info.put("specifications", material.getSpecifications());
                        return info;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(materialInfoList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving product materials: " + e.getMessage()));
        }
    }

    // Helper method to create material info for fallback case
    private Map<String, Object> createMaterialInfo(String name, String description, String unit) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", null);  // No ID for these fallback materials
        info.put("name", name);
        info.put("description", description);
        info.put("unit", unit);
        info.put("specifications", "Standard");
        return info;
    }
}