package com.manublock.backend.services;

import com.manublock.backend.models.Chains;
import com.manublock.backend.models.Nodes;
import com.manublock.backend.models.Users;
import com.manublock.backend.repositories.ChainRepository;
import com.manublock.backend.repositories.NodeRepository;
import com.manublock.backend.repositories.UserRepository;
import com.manublock.backend.repositories.EdgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for supply chain finalization and related operations
 * Now uses admin blockchain service for all operations
 */
@Service
public class SupplyChainFinalizationService {

    @Autowired
    private ChainRepository chainRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminBlockchainService adminBlockchainService;

    /**
     * Finalizes a supply chain, preventing further structure modifications
     * and authorizing all assigned users on the blockchain using admin wallet
     */
    @Transactional
    public Chains finalizeSupplyChain(Long supplyChainId) {
        Chains chain = chainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply chain not found"));

        // 1. Check if the chain has any nodes
        List<Nodes> nodes = nodeRepository.findBySupplyChain_Id(supplyChainId);
        if (nodes.isEmpty()) {
            throw new RuntimeException("Cannot finalize an empty supply chain. Please add nodes first.");
        }

        // 2. Check for orphaned nodes (no connections)
        List<Long> orphanedNodeIds = findOrphanedNodes(supplyChainId);
        if (!orphanedNodeIds.isEmpty()) {
            Nodes firstOrphan = nodeRepository.findById(orphanedNodeIds.get(0)).orElse(null);
            String nodeName = firstOrphan != null ? firstOrphan.getName() : "Unknown";
            throw new RuntimeException("Cannot finalize supply chain with disconnected nodes. Node '" +
                    nodeName + "' has no connections.");
        }

        // 3. Ensure all role-assigned nodes have a user assigned
        List<Nodes> unassignedRoleNodes = nodes.stream()
                .filter(node -> !node.getRole().equals("Unassigned") && node.getAssignedUser() == null)
                .collect(Collectors.toList());

        if (!unassignedRoleNodes.isEmpty()) {
            Nodes firstUnassigned = unassignedRoleNodes.get(0);
            throw new RuntimeException("Node '" + firstUnassigned.getName() +
                    "' has role '" + firstUnassigned.getRole() +
                    "' but no assigned user. Please assign users to all role-specific nodes.");
        }

        chain.setUpdatedAt(new Date());
        chain.setBlockchainStatus("FINALIZED");

        // 5. Register all users on the blockchain using admin wallet
        // 5. Register all users on the blockchain using admin wallet
        nodes.stream()
                .filter(node -> node.getAssignedUser() != null)
                .map(node -> node.getAssignedUser())
                .distinct() // Remove duplicates
                .forEach(user -> {
                    try {
                        // Authorize user in blockchain using admin wallet
                        adminBlockchainService.authorizeParticipant(supplyChainId, user.getId());
                    } catch (Exception e) {
                        // Log error but continue with other users
                        System.err.println("Failed to authorize user " + user.getUsername() +
                                " on blockchain: " + e.getMessage());
                    }
                });

        // Save ONLY the chain with updated status, not modifying nodes or edges
        return chainRepository.save(chain);
    }

    /**
     * Finds nodes with no connections (isolated nodes)
     */
    private List<Long> findOrphanedNodes(Long supplyChainId) {
        List<Nodes> allNodes = nodeRepository.findBySupplyChain_Id(supplyChainId);
        List<Long> allNodeIds = allNodes.stream().map(Nodes::getId).collect(Collectors.toList());

        // Get all nodes that are part of an edge (either source or target)
        List<Long> connectedNodeIds = edgeRepository.findBySupplyChain_Id(supplyChainId).stream()
                .flatMap(edge -> List.of(edge.getSource().getId(), edge.getTarget().getId()).stream())
                .distinct()
                .collect(Collectors.toList());

        // Find nodes that are not in the connected list
        return allNodeIds.stream()
                .filter(id -> !connectedNodeIds.contains(id))
                .collect(Collectors.toList());
    }

    /**
     * Check if the current user is assigned to any node in the supply chain
     */
    public boolean isUserAssignedToChain(Long supplyChainId, Long userId) {
        // Find all nodes in the supply chain that have this user assigned
        List<Nodes> assignedNodes = nodeRepository.findBySupplyChain_IdAndAssignedUser_Id(supplyChainId, userId);
        return !assignedNodes.isEmpty();
    }

    /**
     * Check if a supply chain is finalized
     */
    public boolean isSupplyChainFinalized(Long supplyChainId) {
        Chains chain = chainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply chain not found"));
        return "FINALIZED".equals(chain.getBlockchainStatus());
    }

    /**
     * Get a list of all users assigned to a supply chain
     */
    public List<Users> getAssignedUsers(Long supplyChainId) {
        List<Nodes> nodes = nodeRepository.findBySupplyChain_Id(supplyChainId);

        return nodes.stream()
                .filter(node -> node.getAssignedUser() != null)
                .map(Nodes::getAssignedUser)
                .distinct()
                .collect(Collectors.toList());
    }
}