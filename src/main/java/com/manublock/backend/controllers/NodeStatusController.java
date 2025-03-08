package com.manublock.backend.controllers;

import com.manublock.backend.models.Nodes;
import com.manublock.backend.services.NodeStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/node-status")
@Tag(name = "Node Status API", description = "Operations for managing node statuses in the supply chain visualization")
public class NodeStatusController {

    private final NodeStatusService nodeStatusService;

    @Autowired
    public NodeStatusController(NodeStatusService nodeStatusService) {
        this.nodeStatusService = nodeStatusService;
    }

    @PutMapping("/{nodeId}")
    @Operation(summary = "Update node status", description = "Manually update a node's status")
    public ResponseEntity<?> updateNodeStatus(
            @PathVariable Long nodeId,
            @RequestBody Map<String, String> payload) {
        try {
            String status = payload.get("status");
            nodeStatusService.updateNodeStatus(nodeId, status);
            return ResponseEntity.ok().body("Node status updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating node status: " + e.getMessage());
        }
    }

    @PostMapping("/{nodeId}/associate/{blockchainItemId}")
    @Operation(summary = "Associate node with blockchain item",
            description = "Associate a node with a blockchain item for automatic status updates")
    public ResponseEntity<?> associateNodeWithBlockchainItem(
            @PathVariable Long nodeId,
            @PathVariable Long blockchainItemId) {
        try {
            nodeStatusService.associateNodeWithBlockchainItem(nodeId, blockchainItemId);
            return ResponseEntity.ok().body("Node associated with blockchain item successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error associating node with blockchain item: " + e.getMessage());
        }
    }

    @DeleteMapping("/{nodeId}/associate")
    @Operation(summary = "Remove node association",
            description = "Remove association between a node and blockchain item")
    public ResponseEntity<?> removeNodeAssociation(@PathVariable Long nodeId) {
        try {
            nodeStatusService.removeNodeAssociation(nodeId);
            return ResponseEntity.ok().body("Node association removed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error removing node association: " + e.getMessage());
        }
    }

    @PostMapping("/sync")
    @Operation(summary = "Sync all node statuses",
            description = "Sync all nodes with their associated blockchain items")
    public ResponseEntity<?> syncAllNodeStatuses() {
        try {
            nodeStatusService.syncAllNodeStatuses();
            return ResponseEntity.ok().body("All node statuses synced successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error syncing node statuses: " + e.getMessage());
        }
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get nodes by status",
            description = "Get all nodes with a specific status")
    public ResponseEntity<List<Nodes>> getNodesByStatus(@PathVariable String status) {
        try {
            List<Nodes> nodes = nodeStatusService.getNodesByStatus(status);
            return ResponseEntity.ok(nodes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/supply-chain/{supplyChainId}")
    @Operation(summary = "Get nodes by supply chain",
            description = "Get all nodes for a specific supply chain with their current status")
    public ResponseEntity<List<Nodes>> getNodesWithStatusBySupplyChain(@PathVariable Long supplyChainId) {
        try {
            List<Nodes> nodes = nodeStatusService.getNodesWithStatusBySupplyChain(supplyChainId);
            return ResponseEntity.ok(nodes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/supply-chain/{supplyChainId}/status/{status}")
    @Operation(summary = "Get nodes by supply chain and status",
            description = "Get nodes for a specific supply chain with a specific status")
    public ResponseEntity<List<Nodes>> getNodesBySupplyChainAndStatus(
            @PathVariable Long supplyChainId,
            @PathVariable String status) {
        try {
            List<Nodes> nodes = nodeStatusService.getNodesBySupplyChainAndStatus(supplyChainId, status);
            return ResponseEntity.ok(nodes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{nodeId}/blockchain-item")
    @Operation(summary = "Get associated blockchain item",
            description = "Get the blockchain item ID associated with a node")
    public ResponseEntity<?> getAssociatedBlockchainItemId(@PathVariable Long nodeId) {
        try {
            Long blockchainItemId = nodeStatusService.getAssociatedBlockchainItemId(nodeId);
            if (blockchainItemId == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of("blockchainItemId", blockchainItemId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting associated blockchain item: " + e.getMessage());
        }
    }

    @GetMapping("/associations")
    @Operation(summary = "Get all node associations",
            description = "Get all nodes that have blockchain items associated with them")
    public ResponseEntity<Map<Long, Long>> getAllNodeAssociations() {
        try {
            Map<Long, Long> associations = nodeStatusService.getAllNodeAssociations();
            return ResponseEntity.ok(associations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}