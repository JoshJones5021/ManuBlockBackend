package com.manublock.backend.controllers;

import com.manublock.backend.models.MaterialRequest;
import com.manublock.backend.models.Product;
import com.manublock.backend.models.ProductionBatch;
import com.manublock.backend.services.ManufacturerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manufacturer")
public class ManufacturerController {

    private final ManufacturerService manufacturerService;

    @Autowired
    public ManufacturerController(ManufacturerService manufacturerService) {
        this.manufacturerService = manufacturerService;
    }

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

            @SuppressWarnings("unchecked")
            List<Long> requiredMaterialIds = (List<Long>) payload.get("requiredMaterialIds");

            Product product = manufacturerService.createProduct(
                    name, description, specifications, sku, price,
                    manufacturerId, supplyChainId, requiredMaterialIds);

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

            @SuppressWarnings("unchecked")
            List<Long> requiredMaterialIds = (List<Long>) payload.get("requiredMaterialIds");

            Product product = manufacturerService.updateProduct(
                    productId, name, description, specifications, sku, price, requiredMaterialIds);

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
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deactivating product: " + e.getMessage());
        }
    }

    @PostMapping("/materials/request")
    public ResponseEntity<?> requestMaterials(@RequestBody Map<String, Object> payload) {
        try {
            Long manufacturerId = Long.valueOf(payload.get("manufacturerId").toString());
            Long supplierId = Long.valueOf(payload.get("supplierId").toString());
            Long supplyChainId = Long.valueOf(payload.get("supplyChainId").toString());
            Long orderId = payload.get("orderId") != null ?
                    Long.valueOf(payload.get("orderId").toString()) : null;

            @SuppressWarnings("unchecked")
            List<ManufacturerService.MaterialRequestItemDTO> items =
                    (List<ManufacturerService.MaterialRequestItemDTO>) payload.get("items");

            Date requestedDeliveryDate = payload.get("requestedDeliveryDate") != null ?
                    new Date(Long.parseLong(payload.get("requestedDeliveryDate").toString())) : null;

            String notes = (String) payload.get("notes");

            MaterialRequest materialRequest = manufacturerService.requestMaterials(
                    manufacturerId, supplierId, supplyChainId, orderId,
                    items, requestedDeliveryDate, notes);

            return ResponseEntity.ok(materialRequest);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error requesting materials: " + e.getMessage());
        }
    }

    @PostMapping("/production/batch")
    public ResponseEntity<?> createProductionBatch(@RequestBody Map<String, Object> payload) {
        try {
            Long manufacturerId = Long.valueOf(payload.get("manufacturerId").toString());
            Long productId = Long.valueOf(payload.get("productId").toString());
            Long supplyChainId = Long.valueOf(payload.get("supplyChainId").toString());
            Long orderId = payload.get("orderId") != null ?
                    Long.valueOf(payload.get("orderId").toString()) : null;
            Long quantity = Long.valueOf(payload.get("quantity").toString());

            @SuppressWarnings("unchecked")
            List<ManufacturerService.MaterialBatchItem> materials =
                    (List<ManufacturerService.MaterialBatchItem>) payload.get("materials");

            ProductionBatch batch = manufacturerService.createProductionBatch(
                    manufacturerId, productId, supplyChainId, orderId, quantity, materials);

            return ResponseEntity.ok(batch);
        } catch (Exception e) {
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
            return ResponseEntity.ok(batch);
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
            return ResponseEntity.ok(batch);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error rejecting production batch: " + e.getMessage());
        }
    }

    @GetMapping("/products/{manufacturerId}")
    public ResponseEntity<?> getProductsByManufacturer(@PathVariable Long manufacturerId) {
        try {
            List<Product> products = manufacturerService.getProductsByManufacturer(manufacturerId);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving products: " + e.getMessage());
        }
    }

    @GetMapping("/products/active/{manufacturerId}")
    public ResponseEntity<?> getActiveProductsByManufacturer(@PathVariable Long manufacturerId) {
        try {
            List<Product> products = manufacturerService.getActiveProductsByManufacturer(manufacturerId);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving active products: " + e.getMessage());
        }
    }

    @GetMapping("/materials/requests/{manufacturerId}")
    public ResponseEntity<?> getRequestsByManufacturer(@PathVariable Long manufacturerId) {
        try {
            List<MaterialRequest> requests = manufacturerService.getRequestsByManufacturer(manufacturerId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving material requests: " + e.getMessage());
        }
    }

    @GetMapping("/production/batches/{manufacturerId}")
    public ResponseEntity<?> getBatchesByManufacturer(@PathVariable Long manufacturerId) {
        try {
            List<ProductionBatch> batches = manufacturerService.getBatchesByManufacturer(manufacturerId);
            return ResponseEntity.ok(batches);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving production batches: " + e.getMessage());
        }
    }

    @GetMapping("/orders/{manufacturerId}")
    public ResponseEntity<?> getOrdersByManufacturer(@PathVariable Long manufacturerId) {
        try {
            return ResponseEntity.ok(manufacturerService.getOrdersByManufacturer(manufacturerId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving orders: " + e.getMessage());
        }
    }
}