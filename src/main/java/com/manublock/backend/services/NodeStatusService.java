package com.manublock.backend.services;

import com.manublock.backend.contracts.contract.SmartContract;
import com.manublock.backend.models.Nodes;
import com.manublock.backend.repositories.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.tuples.generated.Tuple7;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final Logger LOGGER = Logger.getLogger(NodeStatusService.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final int MAX_BATCH_SIZE = 10;

    private final NodeRepository nodeRepository;
    private final BlockchainService blockchainService;

    // Using ConcurrentHashMap for thread safety with scheduled tasks
    private final Map<Long, Long> nodeToBlockchainItemMap = new ConcurrentHashMap<>();
    private final Map<Long, Integer> nodeSyncRetryCount = new ConcurrentHashMap<>();

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

        // Only update if status is actually changing to avoid unnecessary DB writes
        if (!status.equals(node.getStatus())) {
            node.setStatus(status);
            nodeRepository.save(node);
            LOGGER.info("Node " + nodeId + " status updated to: " + status);
        }
    }

    /**
     * Associate a node with a blockchain item for status tracking
     * @param nodeId ID of the node
     * @param blockchainItemId ID of the blockchain item
     */
    public void associateNodeWithBlockchainItem(Long nodeId, Long blockchainItemId) {
        // Save the association
        nodeToBlockchainItemMap.put(nodeId, blockchainItemId);
        nodeSyncRetryCount.put(nodeId, 0); // Reset retry count

        // Immediately sync the status
        syncNodeWithBlockchainItem(nodeId, blockchainItemId);

        // Store the association in the database for persistence
        // This would require adding a node_blockchain_items table
        try {
            Nodes node = nodeRepository.findById(nodeId)
                    .orElseThrow(() -> new RuntimeException("Node not found"));

            // Assuming we add a blockchainItemId field to the Nodes entity
            // node.setBlockchainItemId(blockchainItemId);
            // nodeRepository.save(node);

            LOGGER.info("Node " + nodeId + " associated with blockchain item " + blockchainItemId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error persisting node-item association", e);
        }
    }

    /**
     * Remove association between a node and blockchain item
     * @param nodeId ID of the node
     */
    public void removeNodeAssociation(Long nodeId) {
        nodeToBlockchainItemMap.remove(nodeId);
        nodeSyncRetryCount.remove(nodeId);

        // Also remove from database
        try {
            Nodes node = nodeRepository.findById(nodeId)
                    .orElseThrow(() -> new RuntimeException("Node not found"));

            // Assuming we add a blockchainItemId field to the Nodes entity
            // node.setBlockchainItemId(null);
            // nodeRepository.save(node);

            LOGGER.info("Removed blockchain item association from node " + nodeId);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error removing node-item association from database", e);
        }
    }

    /**
     * Sync a node's status with the corresponding blockchain item status
     * @param nodeId ID of the node
     * @param blockchainItemId ID of the blockchain item
     */
    public void syncNodeWithBlockchainItem(Long nodeId, Long blockchainItemId) {
        try {
            SmartContract contract = blockchainService.getContract();
            // Fixed Tuple7 type to match contract return types
            Tuple7<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, String, Boolean> itemDetails =
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

            // Reset retry count on success
            nodeSyncRetryCount.put(nodeId, 0);
        } catch (Exception e) {
            // Implement retry counting
            int retryCount = nodeSyncRetryCount.getOrDefault(nodeId, 0);
            nodeSyncRetryCount.put(nodeId, retryCount + 1);

            if (retryCount >= MAX_RETRIES) {
                LOGGER.log(Level.SEVERE, "Failed to sync node " + nodeId + " after " + MAX_RETRIES + " attempts", e);
                // Optionally, update node status to indicate error
                updateNodeStatus(nodeId, "error");
                // Remove from active tracking to avoid continued failures
                nodeToBlockchainItemMap.remove(nodeId);
                nodeSyncRetryCount.remove(nodeId);
            } else {
                LOGGER.log(Level.WARNING, "Error syncing node " + nodeId + ", retry " + retryCount, e);
            }
        }
    }

    /**
     * Sync all nodes that are associated with blockchain items
     * Process nodes in batches to avoid overwhelming the blockchain provider
     */
    public void syncAllNodeStatuses() {
        // Get a snapshot of current node mappings to avoid ConcurrentModificationException
        Map<Long, Long> nodesToSync = new HashMap<>(nodeToBlockchainItemMap);

        // Process in batches to avoid rate limiting
        List<Long> nodeIds = new ArrayList<>(nodesToSync.keySet());

        for (int i = 0; i < nodeIds.size(); i += MAX_BATCH_SIZE) {
            int endIndex = Math.min(i + MAX_BATCH_SIZE, nodeIds.size());
            List<Long> batch = nodeIds.subList(i, endIndex);

            LOGGER.info("Syncing batch of " + batch.size() + " nodes");

            for (Long nodeId : batch) {
                try {
                    Long blockchainItemId = nodesToSync.get(nodeId);
                    if (blockchainItemId != null) {
                        syncNodeWithBlockchainItem(nodeId, blockchainItemId);
                    }

                    // Small delay between items to avoid rate limiting
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error in batch node sync for node " + nodeId, e);
                }
            }

            // Add delay between batches
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Sync operation interrupted", e);
                break;
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

        // Example implementation:
        // List<NodeItemAssociation> associations = nodeItemAssociationRepository.findAll();
        // for (NodeItemAssociation assoc : associations) {
        //     nodeToBlockchainItemMap.put(assoc.getNodeId(), assoc.getBlockchainItemId());
        //     nodeSyncRetryCount.put(assoc.getNodeId(), 0);
        // }
        // LOGGER.info("Loaded " + associations.size() + " node-item associations");
    }

    /**
     * Scheduled task to sync node statuses with blockchain items
     * Runs every 5 minutes instead of more frequently to reduce API calls
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void scheduledStatusSync() {
        try {
            LOGGER.info("Starting scheduled node status sync");
            syncAllNodeStatuses();
            LOGGER.info("Completed scheduled node status sync");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in scheduled status sync", e);
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