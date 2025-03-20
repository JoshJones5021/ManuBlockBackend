package com.manublock.backend.controllers;

import com.manublock.backend.dto.MaterialDTO;
import com.manublock.backend.dto.ProductDTO;
import com.manublock.backend.dto.TransportDTO;
import com.manublock.backend.models.Items;
import com.manublock.backend.models.Material;
import com.manublock.backend.models.Product;
import com.manublock.backend.models.Transport;
import com.manublock.backend.services.RecyclingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recycle")
public class RecyclingController {

    private final RecyclingService recyclingService;

    @Autowired
    public RecyclingController(RecyclingService recyclingService) {
        this.recyclingService = recyclingService;
    }

    // ====== CUSTOMER ENDPOINTS ======

    /**
     * Mark a product as churned (ready for recycling)
     */
    @PostMapping("/customer/products/{itemId}/churn")
    public ResponseEntity<?> markProductAsChurned(@PathVariable Long itemId, @RequestBody Map<String, Object> payload) {
        try {
            Long customerId = Long.valueOf(payload.get("customerId").toString());
            String notes = (String) payload.get("notes");
            String pickupAddress = (String) payload.get("pickupAddress"); // Extract the new parameter

            Items item = recyclingService.markItemAsChurned(itemId, customerId, notes, pickupAddress);
            return ResponseEntity.ok(Map.of(
                    "message", "Product marked for recycling successfully",
                    "itemId", item.getId(),
                    "status", item.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error marking product for recycling: " + e.getMessage()));
        }
    }

    /**
     * Get all churned products for a customer
     */
    @GetMapping("/customer/products/churned/{customerId}")
    public ResponseEntity<?> getChurnedProducts(@PathVariable Long customerId) {
        try {
            List<Items> items = recyclingService.getChurnedItemsByCustomer(customerId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving churned products: " + e.getMessage()));
        }
    }

    /**
     * Get all recycling transports for a customer
     */
    @GetMapping("/customer/transports/{customerId}")
    public ResponseEntity<?> getCustomerRecyclingTransports(@PathVariable Long customerId) {
        try {
            List<Transport> transports = recyclingService.getRecyclingTransportsByCustomer(customerId);

            // Convert to DTOs to prevent circular references
            List<TransportDTO> transportDTOs = transports.stream()
                    .map(TransportDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(transportDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving recycling transports: " + e.getMessage()));
        }
    }

    // ====== DISTRIBUTOR ENDPOINTS ======

    /**
     * Create a transport for recycling pickup
     */
    @PostMapping("/distributor/transport/create")
    public ResponseEntity<?> createRecyclingTransport(@RequestBody Map<String, Object> payload) {
        try {
            Long distributorId = Long.valueOf(payload.get("distributorId").toString());
            Long customerId = Long.valueOf(payload.get("customerId").toString());
            Long itemId = Long.valueOf(payload.get("itemId").toString());
            Long supplyChainId = Long.valueOf(payload.get("supplyChainId").toString());
            Long manufacturerId = Long.valueOf(payload.get("manufacturerId").toString());

            Date scheduledPickupDate = payload.get("scheduledPickupDate") != null ?
                    new Date(Long.parseLong(payload.get("scheduledPickupDate").toString())) : null;

            Date scheduledDeliveryDate = payload.get("scheduledDeliveryDate") != null ?
                    new Date(Long.parseLong(payload.get("scheduledDeliveryDate").toString())) : null;

            String notes = (String) payload.get("notes");

            Transport transport = recyclingService.createRecyclingTransport(
                    distributorId, customerId, manufacturerId, itemId, supplyChainId,
                    scheduledPickupDate, scheduledDeliveryDate, notes);

            return ResponseEntity.ok(new TransportDTO(transport));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating recycling transport: " + e.getMessage()));
        }
    }

    /**
     * Get all available churned products for recycling pickup
     */
    @GetMapping("/distributor/available-items")
    public ResponseEntity<?> getAvailableChurnedItems() {
        try {
            List<Map<String, Object>> items = recyclingService.getAvailableChurnedItems();
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving available churned items: " + e.getMessage()));
        }
    }

    /**
     * Record pickup of churned item from customer
     */
    @PostMapping("/distributor/transport/{transportId}/pickup")
    public ResponseEntity<?> recordRecyclingPickup(@PathVariable Long transportId) {
        try {
            Transport transport = recyclingService.recordRecyclingPickup(transportId);
            return ResponseEntity.ok(new TransportDTO(transport));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error recording recycling pickup: " + e.getMessage()));
        }
    }

    /**
     * Record delivery of churned item to manufacturer
     */
    @PostMapping("/distributor/transport/{transportId}/delivery")
    public ResponseEntity<?> recordRecyclingDelivery(@PathVariable Long transportId) {
        try {
            Transport transport = recyclingService.recordRecyclingDelivery(transportId);
            return ResponseEntity.ok(new TransportDTO(transport));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error recording recycling delivery: " + e.getMessage()));
        }
    }

    /**
     * Get all recycling transports for a distributor
     */
    @GetMapping("/distributor/transports/{distributorId}")
    public ResponseEntity<?> getDistributorRecyclingTransports(@PathVariable Long distributorId) {
        try {
            List<Transport> transports = recyclingService.getRecyclingTransportsByDistributor(distributorId);

            // Convert to DTOs to prevent circular references
            List<TransportDTO> transportDTOs = transports.stream()
                    .map(TransportDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(transportDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving recycling transports: " + e.getMessage()));
        }
    }

    // ====== MANUFACTURER ENDPOINTS ======

    /**
     * Get all recycled items waiting for processing
     */
    @GetMapping("/manufacturer/pending-items/{manufacturerId}")
    public ResponseEntity<?> getPendingRecycledItems(@PathVariable Long manufacturerId) {
        try {
            List<Map<String, Object>> items = recyclingService.getPendingRecycledItems(manufacturerId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving pending recycled items: " + e.getMessage()));
        }
    }

    /**
     * Process a recycled product into materials
     */
    @PostMapping("/manufacturer/process-to-materials")
    public ResponseEntity<?> processToMaterials(@RequestBody Map<String, Object> payload) {
        try {
            Long manufacturerId = Long.valueOf(payload.get("manufacturerId").toString());
            Long itemId = Long.valueOf(payload.get("itemId").toString());
            Long supplyChainId = Long.valueOf(payload.get("supplyChainId").toString());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> materials = (List<Map<String, Object>>) payload.get("materials");

            List<Material> recycledMaterials = recyclingService.processToMaterials(
                    manufacturerId, itemId, supplyChainId, materials);

            // Convert to DTOs to prevent circular references
            List<MaterialDTO> materialDTOs = recycledMaterials.stream()
                    .map(MaterialDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "message", "Product recycled into materials successfully",
                    "materials", materialDTOs
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error processing recycled product to materials: " + e.getMessage()));
        }
    }

    /**
     * Refurbish a recycled product for resale
     */
    @PostMapping("/manufacturer/refurbish")
    public ResponseEntity<?> refurbishProduct(@RequestBody Map<String, Object> payload) {
        try {
            Long manufacturerId = Long.valueOf(payload.get("manufacturerId").toString());
            Long itemId = Long.valueOf(payload.get("itemId").toString());
            String quality = (String) payload.get("quality");
            String notes = (String) payload.get("notes");

            Product refurbishedProduct = recyclingService.refurbishProduct(
                    manufacturerId, itemId, quality, notes);

            return ResponseEntity.ok(Map.of(
                    "message", "Product refurbished successfully",
                    "product", ProductDTO.fromEntity(refurbishedProduct)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error refurbishing product: " + e.getMessage()));
        }
    }

    /**
     * Get all recycled materials for a manufacturer
     */
    @GetMapping("/manufacturer/materials/{manufacturerId}")
    public ResponseEntity<?> getRecycledMaterials(@PathVariable Long manufacturerId) {
        try {
            List<Material> materials = recyclingService.getRecycledMaterialsByManufacturer(manufacturerId);

            // Convert to DTOs to prevent circular references
            List<MaterialDTO> materialDTOs = materials.stream()
                    .map(MaterialDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(materialDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving recycled materials: " + e.getMessage()));
        }
    }

    /**
     * Get all refurbished products for a manufacturer
     */
    @GetMapping("/manufacturer/products/{manufacturerId}")
    public ResponseEntity<?> getRefurbishedProducts(@PathVariable Long manufacturerId) {
        try {
            List<Product> products = recyclingService.getRefurbishedProductsByManufacturer(manufacturerId);

            // Convert to DTOs to prevent circular references
            List<ProductDTO> productDTOs = products.stream()
                    .map(ProductDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(productDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving refurbished products: " + e.getMessage()));
        }
    }
}