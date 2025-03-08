package com.manublock.backend.services;

import com.manublock.backend.contracts.contract.SmartContract;
import com.manublock.backend.models.Nodes;
import com.manublock.backend.repositories.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.tuples.generated.Tuple7;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for managing node statuses in the supply chain visualization
 * based on blockchain item states.
 *
 * This service provides:
 * 1. Manual status updates for nodes
 * 2. Automatic synchronization with blockchain item statuses
 * 3. Scheduled background updates to keep visualization current
 */
@Service
public class NodeStatusService {
    private final NodeRepository nodeRepository;
    private final BlockchainService blockchainService;

    // Using ConcurrentHashMap for thread safety with scheduled tasks
    private final Map<Long, Long> nodeToBlockchainItemMap = new ConcurrentHashMap<>();

    @Autowired
    public NodeStatusService(NodeRepository nodeRepository, BlockchainService blockchainService) {
        this.nodeRepository = nodeRepository;
        this.blockchainService = blockchainService;
    }

    /**
     * Update the status of a node in the visualization
     * @param nodeId ID of the node to update
     * @param status New status string
     */
    public void updateNodeStatus(Long nodeId, String status) {
        Nodes node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));

        node.setStatus(status);
        nodeRepository.save(node);
    }

    /**
     * Associate a node with a blockchain item for status tracking
     * @param nodeId ID of the node
     * @param blockchainItemId ID of the blockchain item
     */
    public void associateNodeWithBlockchainItem(Long nodeId, Long blockchainItemId) {
        // Save the association
        nodeToBlockchainItemMap.put(nodeId, blockchainItemId);

        // Immediately sync the status
        syncNodeWithBlockchainItem(nodeId, blockchainItemId);
    }

    /**
     * Remove association between a node and blockchain item
     * @param nodeId ID of the node
     */
    public void removeNodeAssociation(Long nodeId) {
        nodeToBlockchainItemMap.remove(nodeId);
    }

    /**
     * Sync a node's status with the corresponding blockchain item status
     * @param nodeId ID of the node
     * @param blockchainItemId ID of the blockchain item
     */
    public void syncNodeWithBlockchainItem(Long nodeId, Long blockchainItemId) {
        try {
            SmartContract contract = blockchainService.getContract();
            Tuple7<BigInteger, String, BigInteger, BigInteger, BigInteger, String, Boolean> itemDetails =
                    contract.getItemDetails(BigInteger.valueOf(blockchainItemId)).send();

            int statusCode = itemDetails.component5().intValue();
            boolean isActive = itemDetails.component7();

            String status;

            if (!isActive) {
                status = "inactive";
            } else {
                // Convert blockchain status code to node status string
                switch (statusCode) {
                    case 0: status = "created"; break;
                    case 1: status = "in_transit"; break;
                    case 2: status = "processing"; break;
                    case 3: status = "completed"; break;
                    case 4: status = "rejected"; break;
                    default: status = "unknown";
                }
            }

            updateNodeStatus(nodeId, status);
        } catch (Exception e) {
            throw new RuntimeException("Error syncing node with blockchain item: " + e.getMessage(), e);
        }
    }

    /**
     * Sync all nodes that are associated with blockchain items
     */
    public void syncAllNodeStatuses() {
        for (Map.Entry<Long, Long> entry : nodeToBlockchainItemMap.entrySet()) {
            try {
                syncNodeWithBlockchainItem(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                System.err.println("Error syncing node " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Load existing node-item associations from the database
     * This should be called at application startup to restore associations
     */
    public void loadNodeAssociations() {
        // This is a placeholder for loading associations from the database
        // In a real implementation, you would load this data from a persistent store
        // For example, you might have a NodeItemAssociation entity in the database

        // Implement this based on your specific persistence strategy
    }

    /**
     * Scheduled task to sync node statuses with blockchain items
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void scheduledStatusSync() {
        try {
            syncAllNodeStatuses();
        } catch (Exception e) {
            System.err.println("Error in scheduled status sync: " + e.getMessage());
        }
    }

    /**
     * Get all nodes with a specific status
     * @param status Status to filter by
     * @return List of nodes with the specified status
     */
    public List<Nodes> getNodesByStatus(String status) {
        return nodeRepository.findByStatus(status);
    }

    /**
     * Get all nodes for a specific supply chain with their current status
     * @param supplyChainId ID of the supply chain
     * @return List of nodes in the supply chain
     */
    public List<Nodes> getNodesWithStatusBySupplyChain(Long supplyChainId) {
        return nodeRepository.findBySupplyChain_Id(supplyChainId);
    }

    /**
     * Get nodes for a specific supply chain with a specific status
     * @param supplyChainId ID of the supply chain
     * @param status Status to filter by
     * @return List of nodes matching the criteria
     */
    public List<Nodes> getNodesBySupplyChainAndStatus(Long supplyChainId, String status) {
        return nodeRepository.findBySupplyChain_IdAndStatus(supplyChainId, status);
    }

    /**
     * Get the blockchain item ID associated with a node
     * @param nodeId ID of the node
     * @return Associated blockchain item ID or null if none
     */
    public Long getAssociatedBlockchainItemId(Long nodeId) {
        return nodeToBlockchainItemMap.get(nodeId);
    }

    /**
     * Get all nodes that have blockchain items associated with them
     * @return Map of node IDs to blockchain item IDs
     */
    public Map<Long, Long> getAllNodeAssociations() {
        return new HashMap<>(nodeToBlockchainItemMap);
    }
}