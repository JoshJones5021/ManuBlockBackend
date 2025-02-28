package com.manublock.backend.services;

import com.manublock.backend.models.Chains;
import com.manublock.backend.models.Edges;
import com.manublock.backend.models.Nodes;
import com.manublock.backend.repositories.EdgeRepository;
import com.manublock.backend.repositories.ChainRepository;
import com.manublock.backend.repositories.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EdgeService {

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private ChainRepository chainRepository;

    @Autowired
    private NodeRepository nodeRepository;

    public Edges addEdge(Long supplyChainId, Edges edge) {
        Chains chain = chainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));
        edge.setSupplyChain(chain);

        // ✅ Ensure source node exists
        Nodes sourceNode = nodeRepository.findById(edge.getSource().getId())
                .orElseThrow(() -> new RuntimeException("Source Node not found"));
        edge.setSource(sourceNode);

        // ✅ Ensure target node exists
        Nodes targetNode = nodeRepository.findById(edge.getTarget().getId())
                .orElseThrow(() -> new RuntimeException("Target Node not found"));
        edge.setTarget(targetNode);

        return edgeRepository.save(edge);
    }

    public List<Edges> getEdgesBySupplyChainId(Long supplyChainId) {
        return edgeRepository.findBySupplyChain_Id(supplyChainId);
    }

    public Edges getEdgeById(Long edgeId) {
        return edgeRepository.findById(edgeId)
                .orElseThrow(() -> new RuntimeException("Edge not found"));
    }

    public Edges updateEdge(Long edgeId, Edges updatedEdge) {
        Edges existingEdge = edgeRepository.findById(edgeId)
                .orElseThrow(() -> new RuntimeException("Edge not found"));

        if (updatedEdge.getSource() != null) {
            Nodes sourceNode = nodeRepository.findById(updatedEdge.getSource().getId())
                    .orElseThrow(() -> new RuntimeException("Source Node not found"));
            existingEdge.setSource(sourceNode);
        }

        if (updatedEdge.getTarget() != null) {
            Nodes targetNode = nodeRepository.findById(updatedEdge.getTarget().getId())
                    .orElseThrow(() -> new RuntimeException("Target Node not found"));
            existingEdge.setTarget(targetNode);
        }

        existingEdge.setAnimated(updatedEdge.getAnimated());
        existingEdge.setStrokeColor(updatedEdge.getStrokeColor());
        existingEdge.setStrokeWidth(updatedEdge.getStrokeWidth());

        return edgeRepository.save(existingEdge);
    }

    public void deleteEdge(Long edgeId) {
        edgeRepository.deleteById(edgeId);
    }
}
