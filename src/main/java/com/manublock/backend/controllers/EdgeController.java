package com.manublock.backend.controllers;

import com.manublock.backend.dto.EdgeResponse;
import com.manublock.backend.models.Edges;
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
    public ResponseEntity<Edges> addEdge(@PathVariable Long supplyChainId, @RequestBody Edges edge) {
        return ResponseEntity.ok(edgeService.addEdge(supplyChainId, edge));
    }

    @GetMapping
    public ResponseEntity<List<EdgeResponse>> getEdges(@PathVariable Long supplyChainId) {
        List<Edges> edges = edgeService.getEdgesBySupplyChainId(supplyChainId);
        List<EdgeResponse> response = edges.stream()
                .map(EdgeResponse::new)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{edgeId}")
    public ResponseEntity<?> getEdgeById(
            @PathVariable Long supplyChainId,
            @PathVariable Long edgeId
    ) {
        Edges edge = edgeService.getEdgeById(edgeId);

        if (edge.getSource() == null || edge.getTarget() == null) {
            System.out.println("⚠️ Edge " + edgeId + " has a null source or target!");
        }

        return ResponseEntity.ok(new EdgeResponse(edge));
    }

    @PutMapping("/{edgeId}")
    public ResponseEntity<Edges> updateEdge(
            @PathVariable Long supplyChainId,
            @PathVariable Long edgeId,
            @RequestBody Edges updatedEdge
    ) {
        return ResponseEntity.ok(edgeService.updateEdge(edgeId, updatedEdge));
    }

    @DeleteMapping("/{edgeId}")
    public ResponseEntity<Void> deleteEdge(@PathVariable Long supplyChainId, @PathVariable Long edgeId) {
        edgeService.deleteEdge(edgeId);
        return ResponseEntity.noContent().build();
    }
}
