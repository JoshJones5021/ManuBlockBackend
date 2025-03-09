package com.manublock.backend.controllers;

import com.manublock.backend.models.Material;
import com.manublock.backend.models.MaterialRequest;
import com.manublock.backend.services.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supplier")
public class SupplierController {

    private final SupplierService supplierService;

    @Autowired
    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping("/materials")
    public ResponseEntity<?> createMaterial(@RequestBody Map<String, Object> payload) {
        try {
            String name = (String) payload.get("name");
            String description = (String) payload.get("description");
            Long quantity = Long.valueOf(payload.get("quantity").toString());
            String unit = (String) payload.get("unit");
            String specifications = (String) payload.get("specifications");
            Long supplierId = Long.valueOf(payload.get("supplierId").toString());
            Long supplyChainId = Long.valueOf(payload.get("supplyChainId").toString());

            Material material = supplierService.createMaterial(
                    name, description, quantity, unit, specifications, supplierId, supplyChainId);

            return ResponseEntity.ok(material);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating material: " + e.getMessage());
        }
    }

    @GetMapping("/material/{id}")
    public ResponseEntity<?> getMaterialById(@PathVariable Long id) {
        try {
            Material material = supplierService.getMaterialById(id);

            if (material == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Material not found");
            }

            return ResponseEntity.ok(material);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving material: " + e.getMessage());
        }
    }

    @PutMapping("/materials/{materialId}")
    public ResponseEntity<?> updateMaterial(
            @PathVariable Long materialId,
            @RequestBody Map<String, Object> payload) {
        try {
            String name = (String) payload.get("name");
            String description = (String) payload.get("description");
            String specifications = (String) payload.get("specifications");
            String unit = (String) payload.get("unit");

            Material material = supplierService.updateMaterial(
                    materialId, name, description, specifications, unit);

            return ResponseEntity.ok(material);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating material: " + e.getMessage());
        }
    }

    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<?> deactivateMaterial(@PathVariable Long materialId) {
        try {
            Material material = supplierService.deactivateMaterial(materialId);
            return ResponseEntity.ok(material);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deactivating material: " + e.getMessage());
        }
    }

    @PostMapping("/requests/{requestId}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long requestId,
            @RequestBody List<SupplierService.MaterialRequestItemApproval> approvals) {
        try {
            MaterialRequest request = supplierService.approveRequest(requestId, approvals);
            return ResponseEntity.ok(request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error approving request: " + e.getMessage());
        }
    }

    @PostMapping("/requests/{requestId}/allocate")
    public ResponseEntity<?> allocateMaterials(@PathVariable Long requestId) {
        try {
            MaterialRequest request = supplierService.allocateMaterials(requestId);
            return ResponseEntity.ok(request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error allocating materials: " + e.getMessage());
        }
    }

    @GetMapping("/materials/{supplierId}")
    public ResponseEntity<?> getMaterialsBySupplier(@PathVariable Long supplierId) {
        try {
            List<Material> materials = supplierService.getMaterialsBySupplier(supplierId);
            return ResponseEntity.ok(materials);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving materials: " + e.getMessage());
        }
    }

    @GetMapping("/materials/active/{supplierId}")
    public ResponseEntity<?> getActiveMaterialsBySupplier(@PathVariable Long supplierId) {
        try {
            List<Material> materials = supplierService.getActiveMaterialsBySupplier(supplierId);
            return ResponseEntity.ok(materials);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving active materials: " + e.getMessage());
        }
    }

    @GetMapping("/requests/pending/{supplierId}")
    public ResponseEntity<?> getPendingRequests(@PathVariable Long supplierId) {
        try {
            List<MaterialRequest> requests = supplierService.getPendingRequests(supplierId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving pending requests: " + e.getMessage());
        }
    }

    @GetMapping("/requests/{supplierId}/{status}")
    public ResponseEntity<?> getRequestsByStatus(
            @PathVariable Long supplierId,
            @PathVariable String status) {
        try {
            List<MaterialRequest> requests = supplierService.getRequestsByStatus(supplierId, status);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving requests: " + e.getMessage());
        }
    }
}