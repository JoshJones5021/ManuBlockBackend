package com.manublock.backend.controllers;

import com.manublock.backend.dto.EdgeResponseDTO;
import com.manublock.backend.models.Edges;
import com.manublock.backend.services.EdgeService;
import com.manublock.backend.services.SupplyChainFinalizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supply-chains/{supplyChainId}/edges")
public class EdgeController {

    private final EdgeService edgeService;
    private final SupplyChainFinalizationService finalizationService;

    @Autowired
    public EdgeController(EdgeService edgeService, SupplyChainFinalizationService finalizationService) {
        this.edgeService = edgeService;
        this.finalizationService = finalizationService;
    }

    @PostMapping
    public ResponseEntity<?> addEdge(@PathVariable Long supplyChainId, @RequestBody Edges edge) {
        try {
            // Check if chain is finalized
            if (finalizationService.isSupplyChainFinalized(supplyChainId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot add connections to a finalized supply chain"));
            }

            // Check for circular reference (self-loop)
            if (edge.getSource() != null && edge.getTarget() != null &&
                    edge.getSource().getId().equals(edge.getTarget().getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot create a connection from a node to itself"));
            }

            Edges createdEdge = edgeService.addEdge(supplyChainId, edge);
            return ResponseEntity.ok(createdEdge);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create connection: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<EdgeResponseDTO>> getEdges(@PathVariable Long supplyChainId) {
        List<Edges> edges = edgeService.getEdgesBySupplyChainId(supplyChainId);
        List<EdgeResponseDTO> response = edges.stream()
                .map(EdgeResponseDTO::new)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{edgeId}")
    public ResponseEntity<?> getEdgeById(
            @PathVariable Long supplyChainId,
            @PathVariable Long edgeId
    ) {
        try {
            Edges edge = edgeService.getEdgeById(edgeId);

            if (edge.getSource() == null || edge.getTarget() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Edge has invalid source or target nodes"));
            }

            return ResponseEntity.ok(new EdgeResponseDTO(edge));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Connection not found: " + e.getMessage()));
        }
    }

    @PutMapping("/{edgeId}")
    public ResponseEntity<?> updateEdge(
            @PathVariable Long supplyChainId,
            @PathVariable Long edgeId,
            @RequestBody Edges updatedEdge
    ) {
        try {
            // Check if chain is finalized
            if (finalizationService.isSupplyChainFinalized(supplyChainId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot update connections in a finalized supply chain"));
            }

            // Check for circular reference
            if (updatedEdge.getSource() != null && updatedEdge.getTarget() != null &&
                    updatedEdge.getSource().getId().equals(updatedEdge.getTarget().getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot create a connection from a node to itself"));
            }

            Edges edge = edgeService.updateEdge(edgeId, updatedEdge);
            return ResponseEntity.ok(edge);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update connection: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{edgeId}")
    public ResponseEntity<?> deleteEdge(@PathVariable Long supplyChainId, @PathVariable Long edgeId) {
        try {
            // Check if chain is finalized
            if (finalizationService.isSupplyChainFinalized(supplyChainId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot delete connections from a finalized supply chain"));
            }

            edgeService.deleteEdge(edgeId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete connection: " + e.getMessage()));
        }
    }

    @GetMapping("/node/{nodeId}")
    public ResponseEntity<?> getEdgesByNode(@PathVariable Long supplyChainId, @PathVariable Long nodeId) {
        try {
            List<Edges> edges = edgeService.getEdgesByNode(nodeId);
            List<EdgeResponseDTO> response = edges.stream()
                    .map(EdgeResponseDTO::new)
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get connections for node: " + e.getMessage()));
        }
    }

    @GetMapping("/check-cycle")
    public ResponseEntity<?> wouldCreateCycle(
            @PathVariable Long supplyChainId,
            @RequestParam Long sourceId,
            @RequestParam Long targetId) {
        try {
            boolean wouldCreateCycle = edgeService.wouldCreateCycle(supplyChainId, sourceId, targetId);
            return ResponseEntity.ok(Map.of("wouldCreateCycle", wouldCreateCycle));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check for cycles: " + e.getMessage()));
        }
    }
}