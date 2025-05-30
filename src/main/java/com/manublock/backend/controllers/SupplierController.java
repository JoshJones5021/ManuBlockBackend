package com.manublock.backend.controllers;

import com.manublock.backend.dto.MaterialDTO;
import com.manublock.backend.dto.MaterialRequestDTO;
import com.manublock.backend.models.Material;
import com.manublock.backend.models.MaterialRequest;
import com.manublock.backend.services.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

            // Convert to MaterialDTO to prevent recursion
            MaterialDTO materialDTO = new MaterialDTO(material);
            return ResponseEntity.ok(materialDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating material: " + e.getMessage());
        }
    }

    @PostMapping("/requests/{requestId}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long requestId,
            @RequestBody List<SupplierService.MaterialRequestItemApproval> approvals) {
        try {
            MaterialRequest request = supplierService.approveRequest(requestId, approvals);
            // Convert to DTO to prevent recursion
            MaterialRequestDTO requestDTO = new MaterialRequestDTO(request);
            return ResponseEntity.ok(requestDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error approving request: " + e.getMessage());
        }
    }

    @PostMapping("/requests/{requestId}/allocate")
    public ResponseEntity<?> allocateMaterials(@PathVariable Long requestId) {
        try {
            MaterialRequest request = supplierService.allocateMaterials(requestId);
            // Convert to DTO to prevent recursion
            MaterialRequestDTO requestDTO = new MaterialRequestDTO(request);
            return ResponseEntity.ok(requestDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error allocating materials: " + e.getMessage());
        }
    }

    @GetMapping("/materials/{supplierId}")
    public ResponseEntity<?> getMaterialsBySupplier(@PathVariable Long supplierId) {
        try {
            List<Material> materials = supplierService.getMaterialsBySupplier(supplierId);
            // Convert to DTO list
            List<MaterialDTO> materialDTOs = materials.stream()
                    .map(MaterialDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(materialDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving materials: " + e.getMessage());
        }
    }

    @GetMapping("/requests/pending/{supplierId}")
    public ResponseEntity<?> getPendingRequests(@PathVariable Long supplierId) {
        try {
            List<MaterialRequest> pendingRequests = supplierService.getPendingRequests(supplierId);

            // Convert to DTOs to prevent circular references
            List<MaterialRequestDTO> requestDTOs = pendingRequests.stream()
                    .map(MaterialRequestDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(requestDTOs);
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

            // Convert to DTOs to prevent circular references
            List<MaterialRequestDTO> requestDTOs = requests.stream()
                    .map(MaterialRequestDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(requestDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving requests: " + e.getMessage());
        }
    }
}