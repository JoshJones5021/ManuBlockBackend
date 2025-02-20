package com.manublock.backend.services;

import com.manublock.backend.models.Edge;
import com.manublock.backend.models.SupplyChain;
import com.manublock.backend.models.SupplyChainNode;
import com.manublock.backend.repositories.EdgeRepository;
import com.manublock.backend.repositories.SupplyChainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class SupplyChainService {

    @Autowired
    private SupplyChainRepository supplyChainRepository;

    @Autowired
    private EdgeRepository edgeRepository;

    private static final Logger logger = LoggerFactory.getLogger(SupplyChainService.class);

    public SupplyChain createSupplyChain(SupplyChain supplyChain, Long createdBy) {
        supplyChain.setCreatedBy(createdBy);
        supplyChain.setCreatedAt(new Date());
        supplyChain.setUpdatedAt(new Date());
        return supplyChainRepository.save(supplyChain);
    }

    public SupplyChain getSupplyChain(Long id) {
        return supplyChainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));
    }

    public List<SupplyChain> getAllSupplyChains() {
        return supplyChainRepository.findAll();
    }

    public SupplyChain updateSupplyChain(Long id, SupplyChain updatedSupplyChain) {
        logger.info("Updating supply chain with ID: {}", id);

        SupplyChain existingSupplyChain = supplyChainRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Supply Chain with ID {} not found", id);
                    return new RuntimeException("Supply Chain not found");
                });

        logger.info("Existing Supply Chain Data: {}", existingSupplyChain);

        if (updatedSupplyChain.getName() != null) {
            logger.info("Updating name: {} -> {}", existingSupplyChain.getName(), updatedSupplyChain.getName());
            existingSupplyChain.setName(updatedSupplyChain.getName());
        }

        if (updatedSupplyChain.getDescription() != null) {
            logger.info("Updating description: {} -> {}", existingSupplyChain.getDescription(), updatedSupplyChain.getDescription());
            existingSupplyChain.setDescription(updatedSupplyChain.getDescription());
        }

        // Ensure nodes are linked to supply chain
        if (updatedSupplyChain.getNodes() != null) {
            existingSupplyChain.getNodes().clear();

            for (SupplyChainNode node : updatedSupplyChain.getNodes()) {
                node.setSupplyChain(existingSupplyChain);

                // Ensure default values
                if (node.getX() == 0) node.setX(100);
                if (node.getY() == 0) node.setY(100);
                if (node.getName() == null) node.setName("Unnamed Node");
                if (node.getRole() == null) node.setRole("Unassigned");
                if (node.getStatus() == null) node.setStatus("pending"); // Ensure status is set

                existingSupplyChain.getNodes().add(node);
            }
        }

        // Ensure edges are linked to supply chain
        if (updatedSupplyChain.getEdges() != null) {
            existingSupplyChain.getEdges().clear();
            for (Edge edge : updatedSupplyChain.getEdges()) {
                edge.setSupplyChain(existingSupplyChain);
                if (edge.getId() == null) {
                    edgeRepository.save(edge); // Save the edge before associating it
                }
                existingSupplyChain.getEdges().add(edge);
            }
        }

        existingSupplyChain.setUpdatedAt(new Date());

        try {
            SupplyChain savedSupplyChain = supplyChainRepository.save(existingSupplyChain);
            logger.info("Successfully updated Supply Chain with ID: {}", savedSupplyChain.getId());
            return savedSupplyChain;
        } catch (Exception e) {
            logger.error("Error updating supply chain: ", e);
            throw new RuntimeException("Failed to update supply chain", e);
        }
    }

    public void deleteSupplyChain(Long id) {
        supplyChainRepository.deleteById(id);
    }
}