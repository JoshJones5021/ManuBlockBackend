package com.manublock.backend.controllers;

import com.manublock.backend.models.SupplyChainNode;
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
    public ResponseEntity<SupplyChainNode> addNode(@PathVariable Long supplyChainId, @RequestBody SupplyChainNode node) {
        SupplyChainNode createdNode = nodeService.addNode(supplyChainId, node);
        return ResponseEntity.ok(createdNode);
    }

    @GetMapping
    public ResponseEntity<List<SupplyChainNode>> getNodes(@PathVariable Long supplyChainId) {
        List<SupplyChainNode> nodes = nodeService.getNodesBySupplyChainId(supplyChainId);
        return ResponseEntity.ok(nodes);
    }

    @PutMapping("/{nodeId}")
    public ResponseEntity<SupplyChainNode> updateNode(
            @PathVariable Long supplyChainId,
            @PathVariable Long nodeId,
            @RequestBody SupplyChainNode updatedNode
    ) {
        SupplyChainNode node = nodeService.updateNode(nodeId, updatedNode);
        return ResponseEntity.ok(node);
    }

    @DeleteMapping("/{nodeId}")
    public ResponseEntity<Void> deleteNode(@PathVariable Long supplyChainId, @PathVariable Long nodeId) {
        nodeService.deleteNode(nodeId);
        return ResponseEntity.noContent().build();
    }
}