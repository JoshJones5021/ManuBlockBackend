package com.manublock.backend.services;

import com.manublock.backend.models.Edge;
import com.manublock.backend.models.SupplyChain;
import com.manublock.backend.models.SupplyChainNode;
import com.manublock.backend.repositories.EdgeRepository;
import com.manublock.backend.repositories.NodeRepository;
import com.manublock.backend.repositories.SupplyChainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class SupplyChainService {

    @Autowired
    private SupplyChainRepository supplyChainRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private EdgeRepository edgeRepository;

    public SupplyChain createSupplyChain(SupplyChain supplyChain, Long createdBy) {
        supplyChain.setCreatedBy(createdBy);
        supplyChain.setCreatedAt(new Date());
        supplyChain.setUpdatedAt(new Date());
        return supplyChainRepository.save(supplyChain);
    }

    public SupplyChain getSupplyChain(Long id) {
        return supplyChainRepository.findById(id).orElseThrow(() -> new RuntimeException("Supply Chain not found"));
    }

    public List<SupplyChain> getAllSupplyChains() {
        return supplyChainRepository.findAll();
    }

    public SupplyChain updateSupplyChain(Long id, SupplyChain updatedSupplyChain) {
        SupplyChain existingSupplyChain = supplyChainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));

        if (updatedSupplyChain.getName() != null) {
            existingSupplyChain.setName(updatedSupplyChain.getName());
        }

        if (updatedSupplyChain.getDescription() != null) {
            existingSupplyChain.setDescription(updatedSupplyChain.getDescription());
        }

        // Update nodes
        if (updatedSupplyChain.getNodes() != null) {
            for (SupplyChainNode updatedNode : updatedSupplyChain.getNodes()) {
                SupplyChainNode existingNode = nodeRepository.findById(updatedNode.getId())
                        .orElseThrow(() -> new RuntimeException("Node not found"));

                // Update only the fields that have changed
                existingNode.setName(updatedNode.getName());
                existingNode.setRole(updatedNode.getRole());
                existingNode.setAssignedUser(updatedNode.getAssignedUser());
                existingNode.setStatus(updatedNode.getStatus());
                existingNode.setX(updatedNode.getX());
                existingNode.setY(updatedNode.getY());

                // Save the updated node
                nodeRepository.save(existingNode);
            }
        }

        // Update edges
        if (updatedSupplyChain.getEdges() != null) {
            existingSupplyChain.getEdges().clear();

            for (Edge edge : updatedSupplyChain.getEdges()) {
                edge.setSupplyChain(existingSupplyChain); // Ensure the relationship is set
                existingSupplyChain.getEdges().add(edge);
            }
        }

        // Update the last modified timestamp
        existingSupplyChain.setUpdatedAt(new Date());

        return supplyChainRepository.save(existingSupplyChain);
    }

    public void deleteSupplyChain(Long id) {
        supplyChainRepository.deleteById(id);
    }
}
