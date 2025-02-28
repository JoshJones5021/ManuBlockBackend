package com.manublock.backend.controllers;

import com.manublock.backend.dto.NodeResponse;
import com.manublock.backend.models.Nodes;
import com.manublock.backend.services.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supply-chains/{supplyChainId}/nodes")
public class NodeController {

    private final NodeService nodeService;

    @Autowired
    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @PostMapping
    public ResponseEntity<Nodes> addNode(@PathVariable Long supplyChainId, @RequestBody Nodes node) {
        Nodes createdNode = nodeService.addNode(supplyChainId, node);
        return ResponseEntity.ok(createdNode);
    }

    @GetMapping
    public ResponseEntity<List<Nodes>> getNodes(@PathVariable Long supplyChainId) {
        List<Nodes> nodes = nodeService.getNodesBySupplyChainId(supplyChainId);
        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/{nodeId}")
    public ResponseEntity<NodeResponse> getNodeById(
            @PathVariable Long supplyChainId,
            @PathVariable Long nodeId
    ) {
        Nodes node = nodeService.getNodeById(nodeId);
        return ResponseEntity.ok(new NodeResponse(node)); // ✅ Pass `Nodes` object directly
    }

    @PutMapping("/{nodeId}")
    public ResponseEntity<Nodes> updateNode(
            @PathVariable Long supplyChainId,
            @PathVariable Long nodeId,
            @RequestBody Nodes updatedNode
    ) {
        Nodes node = nodeService.updateNode(nodeId, updatedNode);
        return ResponseEntity.ok(node);
    }

    @DeleteMapping("/{nodeId}")
    public ResponseEntity<Void> deleteNode(@PathVariable Long supplyChainId, @PathVariable Long nodeId) {
        nodeService.deleteNode(nodeId);
        return ResponseEntity.noContent().build();
    }
}