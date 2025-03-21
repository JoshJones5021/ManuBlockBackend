package com.manublock.backend.controllers;

import com.manublock.backend.models.Nodes;
import com.manublock.backend.models.Users;
import com.manublock.backend.services.NodeService;
import com.manublock.backend.services.SupplyChainFinalizationService;
import com.manublock.backend.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/supply-chains/{supplyChainId}/nodes")
public class NodeController {

    private final NodeService nodeService;
    private final SupplyChainFinalizationService finalizationService;
    private final UserService userService;

    @Autowired
    public NodeController(NodeService nodeService,
                          SupplyChainFinalizationService finalizationService,
                          UserService userService) {
        this.nodeService = nodeService;
        this.finalizationService = finalizationService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> addNode(@PathVariable Long supplyChainId, @RequestBody Nodes node) {
        try {
            // Check if chain is finalized
            if (finalizationService.isSupplyChainFinalized(supplyChainId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot add nodes to a finalized supply chain"));
            }

            // Check if the assigned user exists
            if (node.getAssignedUserId() != null) {
                Optional<Users> user = userService.getUserById(node.getAssignedUserId());
                if (user.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Assigned user does not exist"));
                }

                // Check if user role matches node role (if role is specified)
                if (node.getRole() != null && !node.getRole().equals("Unassigned")) {
                    String userRole = user.get().getRole().name();
                    String nodeRole = node.getRole();

                    // Convert for comparison (e.g., SUPPLIER vs Supplier)
                    if (!userRole.equalsIgnoreCase(nodeRole)) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("error", "User role (" + userRole +
                                        ") does not match node role (" + nodeRole + ")"));
                    }
                }
            }

            // Remove admin ability to set status - will be forced to "pending" in service
            if (node.getStatus() != null && !node.getStatus().equals("pending")) {
                // Don't use admin-provided status, but don't return an error
                // Just inform that the status will be automatically managed
                node.setStatus("pending");
            }

            Nodes createdNode = nodeService.addNode(supplyChainId, node);
            return ResponseEntity.ok(createdNode);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add node: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Nodes>> getNodes(@PathVariable Long supplyChainId) {
        List<Nodes> nodes = nodeService.getNodesBySupplyChainId(supplyChainId);
        return ResponseEntity.ok(nodes);
    }

    @PutMapping("/{nodeId}")
    public ResponseEntity<?> updateNode(
            @PathVariable Long supplyChainId,
            @PathVariable Long nodeId,
            @RequestBody Nodes updatedNode
    ) {
        try {
            // Check if chain is finalized
            if (finalizationService.isSupplyChainFinalized(supplyChainId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot update nodes in a finalized supply chain"));
            }

            // Check if the assigned user exists
            if (updatedNode.getAssignedUserId() != null) {
                Optional<Users> user = userService.getUserById(updatedNode.getAssignedUserId());
                if (user.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Assigned user does not exist"));
                }

                // Check if user role matches node role (if role is specified)
                if (updatedNode.getRole() != null && !updatedNode.getRole().equals("Unassigned")) {
                    String userRole = user.get().getRole().name();
                    String nodeRole = updatedNode.getRole();

                    // Convert for comparison (e.g., SUPPLIER vs Supplier)
                    if (!userRole.equalsIgnoreCase(nodeRole)) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("error", "User role (" + userRole +
                                        ") does not match node role (" + nodeRole + ")"));
                    }
                }
            }

            Nodes existingNode = nodeService.getNodeById(nodeId);
            updatedNode.setStatus(existingNode.getStatus());

            Nodes node = nodeService.updateNode(nodeId, updatedNode);
            return ResponseEntity.ok(node);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update node: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{nodeId}")
    public ResponseEntity<?> deleteNode(@PathVariable Long supplyChainId, @PathVariable Long nodeId) {
        try {
            // Check if chain is finalized
            if (finalizationService.isSupplyChainFinalized(supplyChainId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot delete nodes from a finalized supply chain"));
            }

            nodeService.deleteNode(nodeId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete node: " + e.getMessage()));
        }
    }
}