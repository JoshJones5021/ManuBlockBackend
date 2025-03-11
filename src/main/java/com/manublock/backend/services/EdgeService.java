package com.manublock.backend.services;

import com.manublock.backend.models.Chains;
import com.manublock.backend.models.Edges;
import com.manublock.backend.models.Nodes;
import com.manublock.backend.repositories.EdgeRepository;
import com.manublock.backend.repositories.ChainRepository;
import com.manublock.backend.repositories.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

        // Ensure source node exists
        Nodes sourceNode = nodeRepository.findById(edge.getSource().getId())
                .orElseThrow(() -> new RuntimeException("Source Node not found"));
        edge.setSource(sourceNode);

        // Ensure target node exists
        Nodes targetNode = nodeRepository.findById(edge.getTarget().getId())
                .orElseThrow(() -> new RuntimeException("Target Node not found"));
        edge.setTarget(targetNode);

        // Set default properties if not provided
        if (edge.getAnimated() == null) {
            edge.setAnimated(false);
        }

        if (edge.getStrokeColor() == null) {
            edge.setStrokeColor("#666");
        }

        if (edge.getStrokeWidth() == null) {
            edge.setStrokeWidth(1);
        }

        return edgeRepository.save(edge);
    }

    public List<Edges> getEdgesBySupplyChainId(Long supplyChainId) {
        return edgeRepository.findBySupplyChain_Id(supplyChainId);
    }

    public Edges getEdgeById(Long edgeId) {
        return edgeRepository.findById(edgeId)
                .orElseThrow(() -> new RuntimeException("Edge not found"));
    }

    @Transactional
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

        if (updatedEdge.getAnimated() != null) {
            existingEdge.setAnimated(updatedEdge.getAnimated());
        }

        if (updatedEdge.getStrokeColor() != null) {
            existingEdge.setStrokeColor(updatedEdge.getStrokeColor());
        }

        if (updatedEdge.getStrokeWidth() != null) {
            existingEdge.setStrokeWidth(updatedEdge.getStrokeWidth());
        }

        return edgeRepository.save(existingEdge);
    }

    public void deleteEdge(Long edgeId) {
        edgeRepository.deleteById(edgeId);
    }

    /**
     * Get all edges connected to a specific node (either as source or target)
     */
    public List<Edges> getEdgesByNode(Long nodeId) {
        return edgeRepository.findBySource_IdOrTarget_Id(nodeId, nodeId);
    }

    /**
     * Check if adding an edge would create a cycle in the graph
     */
    public boolean wouldCreateCycle(Long supplyChainId, Long sourceId, Long targetId) {
        // If source and target are the same, it's a self-loop (cycle)
        if (sourceId.equals(targetId)) {
            return true;
        }

        // Get all nodes and edges for this supply chain
        List<Nodes> nodes = nodeRepository.findBySupplyChain_Id(supplyChainId);
        List<Edges> existingEdges = edgeRepository.findBySupplyChain_Id(supplyChainId);

        // Build adjacency list for DFS
        Map<Long, List<Long>> adjacencyList = new HashMap<>();
        for (Nodes node : nodes) {
            adjacencyList.put(node.getId(), new ArrayList<>());
        }

        // Add existing edges to adjacency list
        for (Edges edge : existingEdges) {
            adjacencyList.get(edge.getSource().getId()).add(edge.getTarget().getId());
        }

        // Temporarily add the new edge
        if (!adjacencyList.containsKey(sourceId)) {
            adjacencyList.put(sourceId, new ArrayList<>());
        }
        adjacencyList.get(sourceId).add(targetId);

        // Run DFS to detect cycles
        Set<Long> visited = new HashSet<>();
        Set<Long> recursionStack = new HashSet<>();

        for (Long nodeId : adjacencyList.keySet()) {
            if (hasCycle(nodeId, visited, recursionStack, adjacencyList)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper method for cycle detection using DFS
     */
    private boolean hasCycle(Long nodeId, Set<Long> visited, Set<Long> recursionStack,
                             Map<Long, List<Long>> adjacencyList) {
        // If node is not visited, mark it as visited
        if (!visited.contains(nodeId)) {
            visited.add(nodeId);
            recursionStack.add(nodeId);

            // Visit all neighbors
            List<Long> neighbors = adjacencyList.get(nodeId);
            if (neighbors != null) {
                for (Long neighbor : neighbors) {
                    // If neighbor is not visited and there's a cycle in the subtree
                    if (!visited.contains(neighbor) && hasCycle(neighbor, visited, recursionStack, adjacencyList)) {
                        return true;
                    } else if (recursionStack.contains(neighbor)) {
                        // If neighbor is in recursion stack, there's a cycle
                        return true;
                    }
                }
            }
        }

        // Remove from recursion stack - backtracking
        recursionStack.remove(nodeId);
        return false;
    }
}