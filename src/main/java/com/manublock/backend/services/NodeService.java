package com.manublock.backend.services;

import com.manublock.backend.models.Chains;
import com.manublock.backend.models.Edges;
import com.manublock.backend.models.Nodes;
import com.manublock.backend.models.Users;
import com.manublock.backend.repositories.EdgeRepository;
import com.manublock.backend.repositories.NodeRepository;
import com.manublock.backend.repositories.ChainRepository;
import com.manublock.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NodeService {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ChainRepository chainRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EdgeRepository edgeRepository;

    public Nodes addNode(Long supplyChainId, Nodes node) {
        Chains chain = chainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));

        node.setSupplyChain(chain);

        // Force initial status to "pending" regardless of admin input
        // This ensures consistent initial state and prevents manual status manipulation
        node.setStatus("pending");

        // Ensure assignedUser is properly fetched and set
        if (node.getAssignedUserId() != null) {
            Users user = userRepository.findById(node.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            node.setAssignedUser(user);
        } else {
            node.setAssignedUser(null);
        }

        return nodeRepository.save(node);
    }

    public List<Nodes> getNodesBySupplyChainId(Long supplyChainId) {
        return nodeRepository.findBySupplyChain_Id(supplyChainId);
    }

    public Nodes getNodeById(Long nodeId) {
        return nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));
    }

    @Transactional
    public Nodes updateNode(Long nodeId, Nodes updatedNode) {
        Nodes existingNode = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));

        // If only x and y coordinates are provided, just update those without touching other fields
        boolean hasOnlyCoordinates = isCoordinatesOnlyUpdate(updatedNode);

        if (hasOnlyCoordinates) {
            // Only update the coordinates
            existingNode.setX(updatedNode.getX());
            existingNode.setY(updatedNode.getY());
        } else {
            // Full update - handle all fields

            // Update name if provided
            if (updatedNode.getName() != null) {
                existingNode.setName(updatedNode.getName());
            }

            // Update role if provided
            if (updatedNode.getRole() != null) {
                existingNode.setRole(updatedNode.getRole());
            }

            // Don't allow direct status updates from admin UI
            // Status should only be changed by the NodeStatusService based on blockchain events
            // or through specific system operations

            // Update coordinates
            existingNode.setX(updatedNode.getX());
            existingNode.setY(updatedNode.getY());

            // Handle user assignment
            if (updatedNode.getAssignedUser() != null && updatedNode.getAssignedUser().getId() != null) {
                Users user = userRepository.findById(updatedNode.getAssignedUser().getId())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                existingNode.setAssignedUser(user);
            } else if (updatedNode.getAssignedUserId() != null) {
                Users user = userRepository.findById(updatedNode.getAssignedUserId())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                existingNode.setAssignedUser(user);
            } else {
                // Explicit null assignment means remove the user
                existingNode.setAssignedUser(null);
            }
        }

        return nodeRepository.save(existingNode);
    }

    /**
     * Helper method to determine if the update only contains coordinate changes
     */
    private boolean isCoordinatesOnlyUpdate(Nodes updatedNode) {
        // Check if only x and y are provided (not null or 0)
        // and other fields are default values
        boolean hasCoordinates = updatedNode.getX() != 0 || updatedNode.getY() != 0;
        boolean hasDefaultName = updatedNode.getName() == null || updatedNode.getName().equals("Unnamed Node");
        boolean hasDefaultRole = updatedNode.getRole() == null || updatedNode.getRole().equals("Unassigned");
        boolean hasNoAssignedUser = updatedNode.getAssignedUser() == null && updatedNode.getAssignedUserId() == null;

        return hasCoordinates && hasDefaultName && hasDefaultRole && hasNoAssignedUser;
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        Nodes node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));

        // Find and delete all edges linked to this node
        List<Edges> edges = edgeRepository.findBySource_IdOrTarget_Id(nodeId, nodeId);
        edgeRepository.deleteAll(edges);

        // Now delete the node
        nodeRepository.delete(node);
    }

    /**
     * Get nodes by role in a supply chain
     */
    public List<Nodes> getNodesByRole(Long supplyChainId, String role) {
        return nodeRepository.findBySupplyChain_IdAndRole(supplyChainId, role);
    }

    /**
     * Check if a user is assigned to a specific role in a supply chain
     */
    public boolean isUserAssignedToRole(Long supplyChainId, Long userId, String role) {
        List<Nodes> nodes = nodeRepository.findBySupplyChain_IdAndRole(supplyChainId, role);
        return nodes.stream().anyMatch(node ->
                node.getAssignedUser() != null &&
                        node.getAssignedUser().getId().equals(userId));
    }

    /**
     * Get all nodes assigned to a specific user
     */
    public List<Nodes> getNodesByUser(Long userId) {
        return nodeRepository.findByAssignedUser_Id(userId);
    }

    /**
     * Get all nodes in a supply chain assigned to a specific user
     */
    public List<Nodes> getNodesBySupplyChainAndUser(Long supplyChainId, Long userId) {
        return nodeRepository.findBySupplyChain_IdAndAssignedUser_Id(supplyChainId, userId);
    }
}