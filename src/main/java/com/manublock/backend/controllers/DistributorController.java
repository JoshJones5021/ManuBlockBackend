package com.manublock.backend.controllers;

import com.manublock.backend.dto.MaterialRequestDTO;
import com.manublock.backend.dto.OrderResponseDTO;
import com.manublock.backend.dto.TransportDTO;
import com.manublock.backend.models.MaterialRequest;
import com.manublock.backend.models.Order;
import com.manublock.backend.models.Transport;
import com.manublock.backend.services.DistributorService;
import com.manublock.backend.utils.DTOConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/distributor")
public class DistributorController {

    private final DistributorService distributorService;

    @Autowired
    public DistributorController(DistributorService distributorService) {
        this.distributorService = distributorService;
    }

    @PostMapping("/transport/material")
    public ResponseEntity<?> createMaterialTransport(@RequestBody Map<String, Object> payload) {
        try {
            Long distributorId = Long.valueOf(payload.get("distributorId").toString());
            Long materialRequestId = Long.valueOf(payload.get("materialRequestId").toString());

            Date scheduledPickupDate = payload.get("scheduledPickupDate") != null ?
                    new Date(Long.parseLong(payload.get("scheduledPickupDate").toString())) : null;

            Date scheduledDeliveryDate = payload.get("scheduledDeliveryDate") != null ?
                    new Date(Long.parseLong(payload.get("scheduledDeliveryDate").toString())) : null;

            Transport transport = distributorService.createMaterialTransport(
                    distributorId, materialRequestId, scheduledPickupDate, scheduledDeliveryDate);

            return ResponseEntity.ok(transport);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating material transport: " + e.getMessage());
        }
    }

    @PostMapping("/transport/product")
    public ResponseEntity<?> createProductTransport(@RequestBody Map<String, Object> payload) {
        try {
            Long distributorId = Long.valueOf(payload.get("distributorId").toString());
            Long orderId = Long.valueOf(payload.get("orderId").toString());

            Date scheduledPickupDate = payload.get("scheduledPickupDate") != null ?
                    new Date(Long.parseLong(payload.get("scheduledPickupDate").toString())) : null;

            Date scheduledDeliveryDate = payload.get("scheduledDeliveryDate") != null ?
                    new Date(Long.parseLong(payload.get("scheduledDeliveryDate").toString())) : null;

            Transport transport = distributorService.createProductTransport(
                    distributorId, orderId, scheduledPickupDate, scheduledDeliveryDate);

            return ResponseEntity.ok(transport);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating product transport: " + e.getMessage());
        }
    }

    @PostMapping("/transport/{transportId}/pickup")
    public ResponseEntity<?> recordPickup(@PathVariable Long transportId) {
        try {
            Transport transport = distributorService.recordPickup(transportId);
            return ResponseEntity.ok(transport);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error recording pickup: " + e.getMessage());
        }
    }

    @PostMapping("/transport/{transportId}/delivery")
    public ResponseEntity<?> recordDelivery(@PathVariable Long transportId) {
        try {
            Transport transport = distributorService.recordDelivery(transportId);
            return ResponseEntity.ok(transport);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error recording delivery: " + e.getMessage());
        }
    }

    @GetMapping("/transports/{distributorId}")
    public ResponseEntity<?> getTransportsByDistributor(@PathVariable Long distributorId) {
        try {
            List<Transport> transports = distributorService.getTransportsByDistributor(distributorId);

            // Convert to DTOs to prevent circular references
            List<TransportDTO> transportDTOs = transports.stream()
                    .map(TransportDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(transportDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving transports: " + e.getMessage());
        }
    }

    @GetMapping("/transports/{distributorId}/status/{status}")
    public ResponseEntity<?> getTransportsByStatus(
            @PathVariable Long distributorId,
            @PathVariable String status) {
        try {
            List<Transport> transports = distributorService.getTransportsByDistributorAndStatus(distributorId, status);
            return ResponseEntity.ok(transports);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving transports by status: " + e.getMessage());
        }
    }

    @GetMapping("/transports/{distributorId}/type/{type}")
    public ResponseEntity<?> getTransportsByType(
            @PathVariable Long distributorId,
            @PathVariable String type) {
        try {
            List<Transport> transports = distributorService.getTransportsByType(distributorId, type);
            return ResponseEntity.ok(transports);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving transports by type: " + e.getMessage());
        }
    }

    @GetMapping("/transports/source/{sourceId}")
    public ResponseEntity<?> getTransportsBySource(@PathVariable Long sourceId) {
        try {
            List<Transport> transports = distributorService.getTransportsBySource(sourceId);
            return ResponseEntity.ok(transports);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving transports by source: " + e.getMessage());
        }
    }

    @GetMapping("/materials/ready")
    public ResponseEntity<?> getReadyMaterialRequests() {
        try {
            List<MaterialRequest> requests = distributorService.getReadyMaterialRequests();

            // Convert to DTOs to prevent circular references
            List<MaterialRequestDTO> requestDTOs = requests.stream()
                    .map(MaterialRequestDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(requestDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving ready material requests: " + e.getMessage());
        }
    }

    @GetMapping("/orders/ready")
    public ResponseEntity<?> getReadyOrders() {
        try {
            List<Order> orders = distributorService.getReadyOrders();

            // Convert to DTOs to prevent circular references
            List<OrderResponseDTO> orderDTOs = orders.stream()
                    .map(DTOConverter::convertToOrderDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(orderDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving ready orders: " + e.getMessage());
        }
    }
}