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

        // ✅ Ensure assignedUser is properly fetched and set
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

    public Nodes updateNode(Long nodeId, Nodes updatedNode) {
        Nodes existingNode = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("❌ Node not found"));

        existingNode.setName(updatedNode.getName());
        existingNode.setRole(updatedNode.getRole());
        existingNode.setStatus(updatedNode.getStatus());
        existingNode.setX(updatedNode.getX());
        existingNode.setY(updatedNode.getY());

        // ✅ Ensure assignedUser is properly fetched from the database
        if (updatedNode.getAssignedUser() != null && updatedNode.getAssignedUser().getId() != null) {
            Users user = userRepository.findById(updatedNode.getAssignedUser().getId())
                    .orElseThrow(() -> new RuntimeException("❌ User not found"));
            existingNode.setAssignedUser(user);
        } else {
            existingNode.setAssignedUser(null); // Allow unassigning user if needed
        }

        Nodes savedNode = nodeRepository.save(existingNode);
        System.out.println("✅ Updated node successfully: " + savedNode);

        return savedNode;
    }

    public void deleteNode(Long nodeId) {
        Nodes node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));

        // ✅ Find and delete all edges linked to this node
        List<Edges> edges = edgeRepository.findBySource_IdOrTarget_Id(nodeId, nodeId);
        edgeRepository.deleteAll(edges);

        // ✅ Now delete the node
        nodeRepository.delete(node);
    }
}