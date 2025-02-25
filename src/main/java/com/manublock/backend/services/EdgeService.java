package com.manublock.backend.services;

import com.manublock.backend.models.Edge;
import com.manublock.backend.repositories.EdgeRepository;
import com.manublock.backend.repositories.SupplyChainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EdgeService {

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private SupplyChainRepository supplyChainRepository;

    public Edge addEdge(Long supplyChainId, Edge edge) {
        edge.setSupplyChain(supplyChainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found")));
        return edgeRepository.save(edge);
    }

    public List<Edge> getEdgesBySupplyChainId(Long supplyChainId) {
        return edgeRepository.findBySupplyChainId(supplyChainId);
    }

    public Edge updateEdge(Long edgeId, Edge updatedEdge) {
        Edge existingEdge = edgeRepository.findById(edgeId)
                .orElseThrow(() -> new RuntimeException("Edge not found"));

        existingEdge.setSource(updatedEdge.getSource());
        existingEdge.setTarget(updatedEdge.getTarget());
        existingEdge.setAnimated(updatedEdge.getAnimated());
        existingEdge.setStrokeColor(updatedEdge.getStrokeColor());
        existingEdge.setStrokeWidth(updatedEdge.getStrokeWidth());

        return edgeRepository.save(existingEdge);
    }

    public void deleteEdge(Long edgeId) {
        edgeRepository.deleteById(edgeId);
    }
}
