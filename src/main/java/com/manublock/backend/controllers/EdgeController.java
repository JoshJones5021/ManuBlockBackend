package com.manublock.backend.controllers;

import com.manublock.backend.models.Edge;
import com.manublock.backend.services.EdgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supply-chains/{supplyChainId}/edges")
public class EdgeController {

    private final EdgeService edgeService;

    @Autowired
    public EdgeController(EdgeService edgeService) {
        this.edgeService = edgeService;
    }

    @PostMapping
    public ResponseEntity<Edge> addEdge(@PathVariable Long supplyChainId, @RequestBody Edge edge) {
        return ResponseEntity.ok(edgeService.addEdge(supplyChainId, edge));
    }

    @GetMapping
    public ResponseEntity<List<Edge>> getEdges(@PathVariable Long supplyChainId) {
        return ResponseEntity.ok(edgeService.getEdgesBySupplyChainId(supplyChainId));
    }

    @PutMapping("/{edgeId}")
    public ResponseEntity<Edge> updateEdge(
            @PathVariable Long supplyChainId,
            @PathVariable Long edgeId,
            @RequestBody Edge updatedEdge
    ) {
        return ResponseEntity.ok(edgeService.updateEdge(edgeId, updatedEdge));
    }

    @DeleteMapping("/{edgeId}")
    public ResponseEntity<Void> deleteEdge(@PathVariable Long supplyChainId, @PathVariable Long edgeId) {
        edgeService.deleteEdge(edgeId);
        return ResponseEntity.noContent().build();
    }
}
